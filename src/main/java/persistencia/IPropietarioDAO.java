package persistencia;

import entidad.PropietarioDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IPropietarioDAO {
    void crear(PropietarioDTO propietario) throws SQLException;
    Optional<PropietarioDTO> buscarPorId(int idPropietario) throws SQLException;
    List<PropietarioDTO> listar() throws SQLException;
    boolean actualizar(PropietarioDTO propietario) throws SQLException;
    boolean eliminar(int idPropietario) throws SQLException;
}
