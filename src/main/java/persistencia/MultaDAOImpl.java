package persistencia;

import entidad.MultaDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultaDAOImpl implements IMultaDAO {
    private static final String COLUMNAS =
            "id_multa, codigo_infraccion, descripcion, valor, fecha, id_vehiculo";

    @Override
    public void crear(MultaDTO multa) throws SQLException {
        String sql = "INSERT INTO multas (codigo_infraccion, descripcion, valor, fecha, id_vehiculo) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id_multa";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            asignar(statement, multa);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    multa.setIdMulta(result.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<MultaDTO> buscarPorId(int idMulta) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM multas WHERE id_multa = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idMulta);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<MultaDTO> listar() throws SQLException {
        List<MultaDTO> multas = new ArrayList<>();
        String sql = "SELECT " + COLUMNAS + " FROM multas ORDER BY id_multa";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                multas.add(mapear(result));
            }
        }
        return multas;
    }

    @Override
    public boolean actualizar(MultaDTO multa) throws SQLException {
        String sql = "UPDATE multas SET codigo_infraccion = ?, descripcion = ?, valor = ?, "
                + "fecha = ?, id_vehiculo = ? WHERE id_multa = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            asignar(statement, multa);
            statement.setInt(6, multa.getIdMulta());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean eliminar(int idMulta) throws SQLException {
        String sql = "DELETE FROM multas WHERE id_multa = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idMulta);
            return statement.executeUpdate() == 1;
        }
    }

    private void asignar(PreparedStatement statement, MultaDTO multa) throws SQLException {
        statement.setString(1, multa.getCodigoInfraccion());
        statement.setString(2, multa.getDescripcion());
        statement.setBigDecimal(3, java.math.BigDecimal.valueOf(multa.getValor()));
        statement.setDate(4, multa.getFecha());
        statement.setInt(5, multa.getIdVehiculo());
    }

    private MultaDTO mapear(ResultSet result) throws SQLException {
        MultaDTO multa = new MultaDTO();
        multa.setIdMulta(result.getInt("id_multa"));
        multa.setCodigoInfraccion(result.getString("codigo_infraccion"));
        multa.setDescripcion(result.getString("descripcion"));
        multa.setValor(result.getBigDecimal("valor").doubleValue());
        multa.setFecha(result.getDate("fecha"));
        multa.setIdVehiculo(result.getInt("id_vehiculo"));
        return multa;
    }
}
