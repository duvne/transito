package servicio;

import entidad.MultaDTO;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import persistencia.IMultaDAO;

public class MultaServicio {
    private final IMultaDAO multaDAO;

    public MultaServicio(IMultaDAO multaDAO) {
        this.multaDAO = multaDAO;
    }

    public void crear(MultaDTO multa) throws SQLException {
        validar(multa);
        multaDAO.crear(multa);
    }

    public Optional<MultaDTO> buscarPorId(int idMulta) throws SQLException {
        validarId(idMulta);
        return multaDAO.buscarPorId(idMulta);
    }

    public List<MultaDTO> listar() throws SQLException {
        return multaDAO.listar();
    }

    public boolean actualizar(MultaDTO multa) throws SQLException {
        validar(multa);
        validarId(multa.getIdMulta());
        return multaDAO.actualizar(multa);
    }

    public boolean eliminar(int idMulta) throws SQLException {
        validarId(idMulta);
        return multaDAO.eliminar(idMulta);
    }

    private void validar(MultaDTO multa) {
        if (multa == null) {
            throw new IllegalArgumentException("La multa es obligatoria");
        }
        validarTexto(multa.getCodigoInfraccion(), "El código de infracción");
        validarTexto(multa.getDescripcion(), "La descripción");
        if (!Double.isFinite(multa.getValor()) || multa.getValor() < 0) {
            throw new IllegalArgumentException("El valor debe ser un número no negativo");
        }
        if (multa.getFecha() == null) {
            throw new IllegalArgumentException("La fecha de la multa es obligatoria");
        }
        if (multa.getIdVehiculo() <= 0) {
            throw new IllegalArgumentException("El id del vehículo debe ser positivo");
        }
        if (multa.getIdMulta() < 0) {
            throw new IllegalArgumentException("El id de la multa no puede ser negativo");
        }
    }

    private void validarId(int idMulta) {
        if (idMulta <= 0) {
            throw new IllegalArgumentException("El id de la multa debe ser positivo");
        }
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
    }
}
