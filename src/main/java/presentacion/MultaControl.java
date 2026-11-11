package presentacion;

import entidad.MultaDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import persistencia.MultaDAOImpl;
import servicio.MultaServicio;

public class MultaControl {
    private final MultaServicio multaServicio;

    public MultaControl() {
        this(new MultaServicio(new MultaDAOImpl()));
    }

    public MultaControl(MultaServicio multaServicio) {
        this.multaServicio = multaServicio;
    }

    public void crear(MultaDTO multa) throws SQLException {
        multaServicio.crear(multa);
    }

    public Optional<MultaDTO> buscarPorId(int idMulta) throws SQLException {
        return multaServicio.buscarPorId(idMulta);
    }

    public List<MultaDTO> listar() throws SQLException {
        return multaServicio.listar();
    }

    public boolean actualizar(MultaDTO multa) throws SQLException {
        return multaServicio.actualizar(multa);
    }

    public boolean eliminar(int idMulta) throws SQLException {
        return multaServicio.eliminar(idMulta);
    }
}
