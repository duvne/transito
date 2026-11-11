package persistencia;

import entidad.MultaDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IMultaDAO {
    void crear(MultaDTO multa) throws SQLException;
    Optional<MultaDTO> buscarPorId(int idMulta) throws SQLException;
    List<MultaDTO> listar() throws SQLException;
    boolean actualizar(MultaDTO multa) throws SQLException;
    boolean eliminar(int idMulta) throws SQLException;
}
