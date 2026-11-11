package presentacion;

import entidad.PropietarioDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import persistencia.PropietarioDAOImpl;
import servicio.PropietarioServicio;

public class PropietarioControl {
    private final PropietarioServicio propietarioServicio;

    public PropietarioControl() {
        this(new PropietarioServicio(new PropietarioDAOImpl()));
    }

    public PropietarioControl(PropietarioServicio propietarioServicio) {
        this.propietarioServicio = propietarioServicio;
    }

    public void crear(PropietarioDTO propietario) throws SQLException {
        propietarioServicio.crear(propietario);
    }

    public Optional<PropietarioDTO> buscarPorId(int idPropietario) throws SQLException {
        return propietarioServicio.buscarPorId(idPropietario);
    }

    public List<PropietarioDTO> listar() throws SQLException {
        return propietarioServicio.listar();
    }

    public boolean actualizar(PropietarioDTO propietario) throws SQLException {
        return propietarioServicio.actualizar(propietario);
    }

    public boolean eliminar(int idPropietario) throws SQLException {
        return propietarioServicio.eliminar(idPropietario);
    }
}
