package servicio;

import entidad.PropietarioDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import persistencia.IPropietarioDAO;

public class PropietarioServicio {
    private final IPropietarioDAO propietarioDAO;

    public PropietarioServicio(IPropietarioDAO propietarioDAO) {
        this.propietarioDAO = propietarioDAO;
    }

    public void crear(PropietarioDTO propietario) throws SQLException {
        validar(propietario);
        propietarioDAO.crear(propietario);
    }

    public Optional<PropietarioDTO> buscarPorId(int idPropietario) throws SQLException {
        validarId(idPropietario);
        return propietarioDAO.buscarPorId(idPropietario);
    }

    public List<PropietarioDTO> listar() throws SQLException {
        return propietarioDAO.listar();
    }

    public boolean actualizar(PropietarioDTO propietario) throws SQLException {
        validar(propietario);
        validarId(propietario.getIdPropietario());
        return propietarioDAO.actualizar(propietario);
    }

    public boolean eliminar(int idPropietario) throws SQLException {
        validarId(idPropietario);
        return propietarioDAO.eliminar(idPropietario);
    }

    private void validar(PropietarioDTO propietario) {
        if (propietario == null) {
            throw new IllegalArgumentException("El propietario es obligatorio");
        }
        validarTexto(propietario.getCedula(), "La cédula");
        validarTexto(propietario.getNombres(), "Los nombres");
        validarTexto(propietario.getApellidos(), "Los apellidos");
    }

    private void validarId(int idPropietario) {
        if (idPropietario <= 0) {
            throw new IllegalArgumentException("El id del propietario debe ser positivo");
        }
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
    }
}
