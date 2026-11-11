package servicio;

import entidad.VehiculoDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import persistencia.IVehiculoDAO;

public class VehiculoServicio {
    private final IVehiculoDAO vehiculoDAO;

    public VehiculoServicio(IVehiculoDAO vehiculoDAO) {
        this.vehiculoDAO = vehiculoDAO;
    }

    public void crear(VehiculoDTO vehiculo) throws SQLException {
        validar(vehiculo);
        vehiculoDAO.crear(vehiculo);
    }

    public Optional<VehiculoDTO> buscarPorPlaca(String placa) throws SQLException {
        validarTexto(placa, "La placa");
        return vehiculoDAO.buscarPorPlaca(placa);
    }

    public Optional<VehiculoDTO> buscarPorId(int idVehiculo) throws SQLException {
        validarId(idVehiculo);
        return vehiculoDAO.buscarPorId(idVehiculo);
    }

    public List<VehiculoDTO> listar() throws SQLException {
        return vehiculoDAO.listar();
    }

    public boolean actualizar(VehiculoDTO vehiculo) throws SQLException {
        validar(vehiculo);
        return vehiculoDAO.actualizar(vehiculo);
    }

    public boolean eliminar(int idVehiculo) throws SQLException {
        validarId(idVehiculo);
        return vehiculoDAO.eliminar(idVehiculo);
    }

    private void validar(VehiculoDTO vehiculo) {
        if (vehiculo == null) {
            throw new IllegalArgumentException("El vehículo es obligatorio");
        }
        validarTexto(vehiculo.getPlaca(), "La placa");
        validarTexto(vehiculo.getMarca(), "La marca");
        validarTexto(vehiculo.getModelo(), "El modelo");
        int anioActual = java.time.Year.now().getValue();
        if ((vehiculo.getAnio() != 0 && vehiculo.getAnio() < 1886)
                || vehiculo.getAnio() > anioActual + 1) {
            throw new IllegalArgumentException("El año del vehículo no es válido");
        }
        if (vehiculo.getIdPropietario() <= 0) {
            throw new IllegalArgumentException("El id del propietario debe ser positivo");
        }
        if (vehiculo.getIdVehiculo() < 0) {
            throw new IllegalArgumentException("El id del vehículo no puede ser negativo");
        }
    }

    private void validarId(int idVehiculo) {
        if (idVehiculo <= 0) {
            throw new IllegalArgumentException("El id del vehículo debe ser positivo");
        }
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
    }
}
