package persistencia;

import entidad.VehiculoDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehiculoDAOImpl implements IVehiculoDAO {
    private static final String SELECT_VEHICULO =
            "SELECT v.id_vehiculo, v.placa, v.marca, v.modelo, v.año, v.propietario_id "
            + "FROM vehiculos v ";

    @Override
    public void crear(VehiculoDTO vehiculo) throws SQLException {
        String sql = "INSERT INTO vehiculos (placa, marca, modelo, año, propietario_id) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id_vehiculo";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vehiculo.getPlaca());
            statement.setString(2, vehiculo.getMarca());
            statement.setString(3, vehiculo.getModelo());
            statement.setInt(4, vehiculo.getAño());
            statement.setInt(5, vehiculo.getIdPropietario());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    vehiculo.setIdVehiculo(result.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<VehiculoDTO> buscarPorPlaca(String placa) throws SQLException {
        String sql = SELECT_VEHICULO + "WHERE v.placa = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, placa);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<VehiculoDTO> buscarPorId(int idVehiculo) throws SQLException {
        String sql = SELECT_VEHICULO + "WHERE v.id_vehiculo = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idVehiculo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<VehiculoDTO> listar() throws SQLException {
        List<VehiculoDTO> vehiculos = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_VEHICULO + "ORDER BY v.placa");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                vehiculos.add(mapear(result));
            }
        }
        return vehiculos;
    }

    @Override
    public boolean actualizar(VehiculoDTO vehiculo) throws SQLException {
        String sql = "UPDATE vehiculos SET placa = ?, marca = ?, modelo = ?, año = ?, "
                + "propietario_id = ? WHERE id_vehiculo = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, vehiculo.getPlaca());
            statement.setString(2, vehiculo.getMarca());
            statement.setString(3, vehiculo.getModelo());
            statement.setInt(4, vehiculo.getAño());
            statement.setInt(5, vehiculo.getIdPropietario());
            statement.setInt(6, vehiculo.getIdVehiculo());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean eliminar(int idVehiculo) throws SQLException {
        String sql = "DELETE FROM vehiculos WHERE id_vehiculo = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idVehiculo);
            return statement.executeUpdate() == 1;
        }
    }

    private VehiculoDTO mapear(ResultSet result) throws SQLException {
        VehiculoDTO vehiculo = new VehiculoDTO();
        vehiculo.setIdVehiculo(result.getInt("id_vehiculo"));
        vehiculo.setPlaca(result.getString("placa"));
        vehiculo.setMarca(result.getString("marca"));
        vehiculo.setModelo(result.getString("modelo"));
        vehiculo.setAño(result.getInt("año"));
        vehiculo.setIdPropietario(result.getInt("propietario_id"));
        return vehiculo;
    }
}
