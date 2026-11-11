package persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public final class DatabaseConnection {
    private static final String URL_VARIABLE = "TRANSITO_DB_URL";
    private static final String USER_VARIABLE = "TRANSITO_DB_USER";
    private static final String PASSWORD_VARIABLE = "TRANSITO_DB_PASSWORD";
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "12345678";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        String url = environmentVariableOrDefault(URL_VARIABLE, DEFAULT_URL);
        String user = environmentVariableOrDefault(USER_VARIABLE, DEFAULT_USER);
        String password = environmentVariableOrDefault(PASSWORD_VARIABLE, DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }

    public static void initializeSchema() throws SQLException, IOException {
        try (InputStream schema = DatabaseConnection.class.getResourceAsStream("/db/schema.sql")) {
            if (schema == null) {
                throw new IOException("No se encontró el recurso /db/schema.sql.");
            }

            String[] statements = new String(schema.readAllBytes(), StandardCharsets.UTF_8)
                    .split(";");
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);
                try {
                    boolean migrarPropietarios = esEsquemaAnterior(connection);
                    if (migrarPropietarios) {
                        try (Statement statement = connection.createStatement()) {
                            try (ResultSet legacy = statement.executeQuery(
                                    "SELECT to_regclass('public.propietarios_legacy')")) {
                                legacy.next();
                                if (legacy.getString(1) != null) {
                                    throw new SQLException(
                                            "Ya existe propietarios_legacy; revisa la migración anterior.");
                                }
                            }
                            statement.execute("ALTER TABLE propietarios RENAME TO propietarios_legacy");
                        }
                    }

                    try (Statement statement = connection.createStatement()) {
                        for (String sql : statements) {
                            if (!sql.isBlank()) {
                                statement.execute(sql);
                            }
                        }
                    }

                    if (migrarPropietarios) {
                        migrarPropietarios(connection);
                    }
                    migrarVehiculos(connection);
                    migrarMultas(connection);
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }
        }
    }

    private static boolean esEsquemaAnterior(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        boolean existeTabla = false;
        try (ResultSet tables = metadata.getTables(null, "public", "propietarios", new String[]{"TABLE"})) {
            existeTabla = tables.next();
        }
        return existeTabla && !tieneColumna(metadata, "propietarios", "id_propietario");
    }

    private static boolean tieneColumna(
            DatabaseMetaData metadata, String tabla, String columna) throws SQLException {
        try (ResultSet columns = metadata.getColumns(null, "public", tabla, null)) {
            while (columns.next()) {
                if (columna.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void migrarPropietarios(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO propietarios (cedula, nombres, apellidos) "
                    + "SELECT id, "
                    + "COALESCE(NULLIF(split_part(btrim(nombre), ' ', 1), ''), btrim(nombre)), "
                    + "COALESCE(NULLIF(btrim(substr(btrim(nombre), "
                    + "length(split_part(btrim(nombre), ' ', 1)) + 1)), ''), '') "
                    + "FROM propietarios_legacy");
        }

        DatabaseMetaData metadata = connection.getMetaData();
        if (!tieneColumna(metadata, "vehiculos", "propietario_id")) {
            return;
        }
        int tipoColumna;
        try (ResultSet columns = metadata.getColumns(null, "public", "vehiculos", "propietario_id")) {
            if (!columns.next()) {
                return;
            }
            tipoColumna = columns.getInt("DATA_TYPE");
        }
        if (tipoColumna != Types.VARCHAR && tipoColumna != Types.CHAR
                && tipoColumna != Types.LONGVARCHAR) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE vehiculos DROP CONSTRAINT IF EXISTS fk_vehiculo_propietario");
            statement.execute("ALTER TABLE vehiculos ADD COLUMN propietario_id_nuevo INTEGER");
            statement.executeUpdate(
                    "UPDATE vehiculos v SET propietario_id_nuevo = p.id_propietario "
                    + "FROM propietarios p WHERE p.cedula = v.propietario_id");
            try (ResultSet unmatched = statement.executeQuery(
                    "SELECT COUNT(*) FROM vehiculos WHERE propietario_id_nuevo IS NULL")) {
                unmatched.next();
                if (unmatched.getLong(1) > 0) {
                    throw new SQLException(
                            "Hay vehículos sin propietario válido; no se migraron los datos.");
                }
            }
            statement.execute("ALTER TABLE vehiculos DROP COLUMN propietario_id");
            statement.execute("ALTER TABLE vehiculos RENAME COLUMN propietario_id_nuevo TO propietario_id");
            statement.execute("ALTER TABLE vehiculos ADD CONSTRAINT fk_vehiculo_propietario "
                    + "FOREIGN KEY (propietario_id) REFERENCES propietarios (id_propietario)");
        }
    }

    private static void migrarVehiculos(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        if (!existeTabla(metadata, "vehiculos")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            if (!tieneColumna(metadata, "vehiculos", "marca")) {
                statement.execute("ALTER TABLE vehiculos ADD COLUMN marca "
                        + "VARCHAR(100) NOT NULL DEFAULT 'Sin especificar'");
            }
            if (!tieneColumna(metadata, "vehiculos", "anio")) {
                statement.execute("ALTER TABLE vehiculos ADD COLUMN anio "
                        + "INTEGER NOT NULL DEFAULT 0");
            }
            if (tieneColumna(metadata, "vehiculos", "id_vehiculo")) {
                return;
            }

            try (ResultSet legacyIdentity = statement.executeQuery(
                    "SELECT to_regclass('public.vehiculos_legacy')")) {
                legacyIdentity.next();
                if (legacyIdentity.getString(1) != null) {
                    throw new SQLException(
                            "Ya existe vehiculos_legacy; revisa la migración anterior.");
                }
            }
            statement.execute("ALTER TABLE multas DROP CONSTRAINT IF EXISTS fk_multa_vehiculo");
            statement.execute("ALTER TABLE vehiculos ADD COLUMN id_vehiculo SERIAL");
            statement.execute("ALTER TABLE vehiculos ADD CONSTRAINT uq_vehiculos_placa UNIQUE (placa)");
            try (ResultSet primaryKey = metadata.getPrimaryKeys(null, "public", "vehiculos")) {
                if (primaryKey.next()) {
                    String constraintName = primaryKey.getString("PK_NAME");
                    if (constraintName != null) {
                        statement.execute("ALTER TABLE vehiculos DROP CONSTRAINT "
                                + quoteIdentifier(constraintName));
                    }
                }
            }
            statement.execute("ALTER TABLE vehiculos ADD CONSTRAINT pk_vehiculos "
                    + "PRIMARY KEY (id_vehiculo)");
            statement.execute("ALTER TABLE multas ADD CONSTRAINT fk_multa_vehiculo "
                    + "FOREIGN KEY (placa_vehiculo) REFERENCES vehiculos (placa)");
            statement.execute("ALTER TABLE vehiculos ADD CONSTRAINT ck_vehiculos_anio "
                    + "CHECK (anio = 0 OR anio >= 1886) NOT VALID");
        }
    }

    private static void migrarMultas(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        if (!existeTabla(metadata, "multas")
                || tieneColumna(metadata, "multas", "id_vehiculo")) {
            return;
        }

        try (Statement statement = connection.createStatement();
             ResultSet legacyTable = statement.executeQuery(
                     "SELECT to_regclass('public.multas_legacy')")) {
            legacyTable.next();
            if (legacyTable.getString(1) != null) {
                throw new SQLException(
                        "Ya existe multas_legacy; revisa la migración anterior.");
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE multas DROP CONSTRAINT IF EXISTS fk_multa_vehiculo");
            statement.execute("ALTER TABLE multas RENAME TO multas_legacy");
            statement.execute("CREATE TABLE multas ("
                    + "id_multa SERIAL PRIMARY KEY, "
                    + "codigo_infraccion VARCHAR(50) NOT NULL, "
                    + "descripcion VARCHAR(500) NOT NULL, "
                    + "valor NUMERIC(12, 2) NOT NULL CHECK (valor >= 0), "
                    + "fecha DATE NOT NULL, "
                    + "id_vehiculo INTEGER NOT NULL, "
                    + "CONSTRAINT fk_multa_vehiculo FOREIGN KEY (id_vehiculo) "
                    + "REFERENCES vehiculos (id_vehiculo))");

            if (!tieneColumna(metadata, "multas_legacy", "placa_vehiculo")
                    || !tieneColumna(metadata, "multas_legacy", "monto")
                    || !tieneColumna(metadata, "multas_legacy", "id_multa")) {
                throw new SQLException(
                        "La tabla multas_legacy no coincide con el esquema conocido; "
                        + "se conservaron los datos sin migrarlos.");
            }
            statement.executeUpdate(
                    "INSERT INTO multas (codigo_infraccion, descripcion, valor, fecha, id_vehiculo) "
                    + "SELECT CAST(m.id_multa AS VARCHAR(50)), 'Migrada desde el esquema anterior', "
                    + "m.monto, CURRENT_DATE, v.id_vehiculo "
                    + "FROM multas_legacy m JOIN vehiculos v ON v.placa = m.placa_vehiculo");
            try (ResultSet counts = statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM multas_legacy) - (SELECT COUNT(*) FROM multas)")) {
                counts.next();
                if (counts.getLong(1) != 0) {
                    throw new SQLException(
                            "Hay multas sin un vehículo asociado; no se migraron todos los registros.");
                }
            }
            statement.execute("SELECT setval(pg_get_serial_sequence('multas', 'id_multa'), "
                    + "COALESCE((SELECT MAX(id_multa) FROM multas), 1), "
                    + "EXISTS (SELECT 1 FROM multas))");
        }
    }

    private static boolean existeTabla(DatabaseMetaData metadata, String tabla) throws SQLException {
        try (ResultSet tables = metadata.getTables(null, "public", tabla, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String environmentVariableOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
