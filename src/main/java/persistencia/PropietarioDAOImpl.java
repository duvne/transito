package persistencia;

import entidad.PropietarioDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PropietarioDAOImpl implements IPropietarioDAO {
    private static final String COLUMNAS =
            "id_propietario, cedula, nombres, apellidos, telefono, email, fecha";

    @Override
    public void crear(PropietarioDTO propietario) throws SQLException {
        String sql = "INSERT INTO propietarios "
                + "(cedula, nombres, apellidos, telefono, email, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_propietario";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, propietario.getCedula());
            statement.setString(2, propietario.getNombres());
            statement.setString(3, propietario.getApellidos());
            statement.setString(4, propietario.getTelefono());
            statement.setString(5, propietario.getEmail());
            statement.setDate(6, propietario.getFecha());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    propietario.setIdPropietario(result.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<PropietarioDTO> buscarPorId(int idPropietario) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM propietarios WHERE id_propietario = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idPropietario);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(mapear(result))
                        : Optional.empty();
            }
        }
    }

    @Override
    public List<PropietarioDTO> listar() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM propietarios ORDER BY id_propietario";
        List<PropietarioDTO> propietarios = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                propietarios.add(mapear(result));
            }
        }
        return propietarios;
    }

    @Override
    public boolean actualizar(PropietarioDTO propietario) throws SQLException {
        String sql = "UPDATE propietarios SET cedula = ?, nombres = ?, apellidos = ?, "
                + "telefono = ?, email = ?, fecha = ? WHERE id_propietario = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, propietario.getCedula());
            statement.setString(2, propietario.getNombres());
            statement.setString(3, propietario.getApellidos());
            statement.setString(4, propietario.getTelefono());
            statement.setString(5, propietario.getEmail());
            statement.setDate(6, propietario.getFecha());
            statement.setInt(7, propietario.getIdPropietario());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean eliminar(int idPropietario) throws SQLException {
        String sql = "DELETE FROM propietarios WHERE id_propietario = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idPropietario);
            return statement.executeUpdate() == 1;
        }
    }

    private PropietarioDTO mapear(ResultSet result) throws SQLException {
        PropietarioDTO propietario = new PropietarioDTO();
        propietario.setIdPropietario(result.getInt("id_propietario"));
        propietario.setCedula(result.getString("cedula"));
        propietario.setNombres(result.getString("nombres"));
        propietario.setApellidos(result.getString("apellidos"));
        propietario.setTelefono(result.getString("telefono"));
        propietario.setEmail(result.getString("email"));
        propietario.setFecha(result.getDate("fecha"));
        return propietario;
    }
}
