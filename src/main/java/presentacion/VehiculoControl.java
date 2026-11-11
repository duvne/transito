package presentacion;

import entidad.VehiculoDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import persistencia.VehiculoDAOImpl;
import servicio.VehiculoServicio;

public class VehiculoControl {
    private final VehiculoServicio vehiculoServicio;

    public VehiculoControl() {
        this(new VehiculoServicio(new VehiculoDAOImpl()));
    }

    public VehiculoControl(VehiculoServicio vehiculoServicio) {
        this.vehiculoServicio = vehiculoServicio;
    }

    public void crear(VehiculoDTO vehiculo) throws SQLException {
        vehiculoServicio.crear(vehiculo);
    }

    public Optional<VehiculoDTO> buscarPorPlaca(String placa) throws SQLException {
        return vehiculoServicio.buscarPorPlaca(placa);
    }

    public Optional<VehiculoDTO> buscarPorId(int idVehiculo) throws SQLException {
        return vehiculoServicio.buscarPorId(idVehiculo);
    }

    public List<VehiculoDTO> listar() throws SQLException {
        return vehiculoServicio.listar();
    }

    public boolean actualizar(VehiculoDTO vehiculo) throws SQLException {
        return vehiculoServicio.actualizar(vehiculo);
    }

    public boolean eliminar(int idVehiculo) throws SQLException {
        return vehiculoServicio.eliminar(idVehiculo);
    }
}
