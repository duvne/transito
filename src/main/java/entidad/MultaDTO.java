package entidad;

public class MultaDTO {
    private int idMulta;
    private String codigoInfraccion;
    private String descripcion;
    private double valor;
    private java.sql.Date fecha;
    private int idVehiculo;

    public MultaDTO() {}

    public int getIdMulta() { return idMulta; }
    public void setIdMulta(int idMulta) { this.idMulta = idMulta; }

    public String getCodigoInfraccion() { return codigoInfraccion; }
    public void setCodigoInfraccion(String codigoInfraccion) { this.codigoInfraccion = codigoInfraccion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public java.sql.Date getFecha() { return fecha; }
    public void setFecha(java.sql.Date fecha) { this.fecha = fecha; }

    public int getIdVehiculo() { return idVehiculo; }
    public void setIdVehiculo(int idVehiculo) { this.idVehiculo = idVehiculo; }
}
