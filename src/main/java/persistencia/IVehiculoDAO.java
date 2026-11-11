package persistencia;

import entidad.VehiculoDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IVehiculoDAO {
    void crear(VehiculoDTO vehiculo) throws SQLException;
    Optional<VehiculoDTO> buscarPorPlaca(String placa) throws SQLException;
    Optional<VehiculoDTO> buscarPorId(int idVehiculo) throws SQLException;
    List<VehiculoDTO> listar() throws SQLException;
    boolean actualizar(VehiculoDTO vehiculo) throws SQLException;
    boolean eliminar(int idVehiculo) throws SQLException;
}
