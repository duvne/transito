package presentacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonParser;
import com.mycompany.mavenproject1.transito;
import entidad.MultaDTO;
import entidad.PropietarioDTO;
import entidad.VehiculoDTO;
import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Optional;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import persistencia.DatabaseConnection;

public final class ServidorWebTransito {
    private static final int FIRST_PORT = Integer.parseInt(
            System.getenv().getOrDefault("TRANSITO_WEB_PORT", "8080"));
    private static final int PORT_ATTEMPTS = 20;
    private static final int MAX_REQUEST_BYTES = 64 * 1024;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new SqlDateAdapter())
            .create();

    private final PropietarioControl propietarios = new PropietarioControl();
    private final VehiculoControl vehiculos = new VehiculoControl();
    private final MultaControl multas = new MultaControl();

    private ServidorWebTransito() {}

    public static void iniciar() {
        try {
            DatabaseConnection.initializeSchema();
            System.out.println("Esquema de PostgreSQL verificado.");
        } catch (IOException e) {
            System.err.println("No se pudo cargar el esquema de PostgreSQL: " + e.getMessage());
            System.exit(1);
            return;
        } catch (SQLException e) {
            System.err.println("No se pudo inicializar PostgreSQL: " + e.getMessage());
            System.err.println("Verifica el servidor, la base de datos y las credenciales.");
            System.exit(1);
            return;
        }

        HttpServer server = null;
        int port = FIRST_PORT;
        try {
            for (int attempt = 0; attempt < PORT_ATTEMPTS; attempt++) {
                port = FIRST_PORT + attempt;
                try {
                    server = HttpServer.create(new InetSocketAddress(port), 0);
                    break;
                } catch (BindException e) {
                    if (attempt == PORT_ATTEMPTS - 1) {
                        throw e;
                    }
                }
            }
            ServidorWebTransito app = new ServidorWebTransito();
            server.createContext("/", app::atender);
            server.start();

            String url = "http://localhost:" + port;
            System.out.println("Interfaz de Tránsito disponible en " + url);
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(java.net.URI.create(url));
                } catch (IOException e) {
                    System.err.println("No se pudo abrir el navegador automáticamente: "
                            + e.getMessage());
                    System.out.println("Abre esta dirección manualmente: " + url);
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor web: " + e.getMessage());
            System.err.println("No hay puertos disponibles entre " + FIRST_PORT + " y "
                    + (FIRST_PORT + PORT_ATTEMPTS - 1)
                    + ". Puedes elegir otro puerto con TRANSITO_WEB_PORT.");
            System.exit(1);
        }
    }

    private void atender(HttpExchange exchange) throws IOException {
        try {
            if ("/".equals(exchange.getRequestURI().getPath())
                    && "GET".equals(exchange.getRequestMethod())) {
                servirPagina(exchange);
                return;
            }
            atenderApi(exchange);
        } catch (JsonParseException | IllegalArgumentException e) {
            responderError(exchange, 400, e.getMessage());
        } catch (SQLException e) {
            int status = "23505".equals(e.getSQLState()) || "23503".equals(e.getSQLState())
                    ? 409 : 500;
            responderError(exchange, status,
                    status == 409 ? "El registro ya existe o está relacionado con otros datos."
                            : "Error al consultar PostgreSQL: " + e.getMessage());
        }
    }

    private void servirPagina(HttpExchange exchange) throws IOException {
        try (InputStream pagina = transito.class.getResourceAsStream("/index.html")) {
            if (pagina == null) {
                responderError(exchange, 500, "No se encontró la interfaz web index.html.");
                return;
            }
            byte[] contenido = pagina.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.sendResponseHeaders(200, contenido.length);
            exchange.getResponseBody().write(contenido);
        } finally {
            exchange.close();
        }
    }

    private void atenderApi(HttpExchange exchange) throws IOException, SQLException {
        String[] partes = exchange.getRequestURI().getPath().split("/", -1);
        if (partes.length < 3 || !"api".equals(partes[1])) {
            responderError(exchange, 404, "Ruta no encontrada.");
            return;
        }

        String recurso = partes[2];
        String metodo = exchange.getRequestMethod();
        String id = partes.length > 3 && !partes[3].isBlank()
                ? URLDecoder.decode(partes[3], StandardCharsets.UTF_8) : null;

        if (partes.length > 4) {
            responderError(exchange, 404, "Ruta no encontrada.");
            return;
        }

        if ("propietarios".equals(recurso)) {
            atenderPropietarios(exchange, metodo, id == null ? null : parseId(id));
        } else if ("vehiculos".equals(recurso)) {
            atenderVehiculos(exchange, metodo, id);
        } else if ("multas".equals(recurso)) {
            atenderMultas(exchange, metodo, id);
        } else {
            responderError(exchange, 404, "Ruta no encontrada.");
        }
    }

    private void atenderPropietarios(HttpExchange exchange, String metodo, Integer id)
            throws IOException, SQLException {
        if ("GET".equals(metodo) && id == null) {
            responder(exchange, 200, propietarios.listar());
        } else if ("POST".equals(metodo) && id == null) {
            JsonObject datos = leerJson(exchange);
            PropietarioDTO propietario = leerPropietario(datos);
            propietarios.crear(propietario);
            responder(exchange, 201, propietario);
        } else if ("PUT".equals(metodo) && id != null) {
            JsonObject datos = leerJson(exchange);
            PropietarioDTO propietario = leerPropietario(datos);
            propietario.setIdPropietario(id);
            if (propietarios.actualizar(propietario)) {
                responder(exchange, 200, propietario);
            } else {
                responderError(exchange, 404, "No se encontró el propietario.");
            }
        } else if ("DELETE".equals(metodo) && id != null) {
            responderEliminacion(exchange, propietarios.eliminar(id), "No se encontró el propietario.");
        } else {
            metodoNoPermitido(exchange);
        }
    }

    private void atenderVehiculos(HttpExchange exchange, String metodo, String id)
            throws IOException, SQLException {
        if ("GET".equals(metodo) && id == null) {
            responder(exchange, 200, vehiculos.listar());
        } else if ("GET".equals(metodo) && id != null) {
            Optional<VehiculoDTO> vehiculo = vehiculos.buscarPorId(parseId(id));
            if (vehiculo.isPresent()) {
                responder(exchange, 200, vehiculo.get());
            } else {
                responderError(exchange, 404, "No se encontró el vehículo.");
            }
        } else if ("POST".equals(metodo) && id == null) {
            JsonObject datos = leerJson(exchange);
            VehiculoDTO vehiculo = leerVehiculo(datos);
            vehiculos.crear(vehiculo);
            responder(exchange, 201, vehiculo);
        } else if ("PUT".equals(metodo) && id != null) {
            JsonObject datos = leerJson(exchange);
            VehiculoDTO vehiculo = leerVehiculo(datos);
            vehiculo.setIdVehiculo(parseId(id));
            if (vehiculos.actualizar(vehiculo)) {
                responder(exchange, 200, vehiculo);
            } else {
                responderError(exchange, 404, "No se encontró el vehículo.");
            }
        } else if ("DELETE".equals(metodo) && id != null) {
            responderEliminacion(exchange, vehiculos.eliminar(parseId(id)), "No se encontró el vehículo.");
        } else {
            metodoNoPermitido(exchange);
        }
    }

    private void atenderMultas(HttpExchange exchange, String metodo, String id)
            throws IOException, SQLException {
        if ("GET".equals(metodo) && id == null) {
            responder(exchange, 200, multas.listar());
        } else if ("GET".equals(metodo) && id != null) {
            Optional<MultaDTO> multa = multas.buscarPorId(parseId(id));
            if (multa.isPresent()) {
                responder(exchange, 200, multa.get());
            } else {
                responderError(exchange, 404, "No se encontró la multa.");
            }
        } else if ("POST".equals(metodo) && id == null) {
            JsonObject datos = leerJson(exchange);
            MultaDTO multa = leerMulta(datos);
            multas.crear(multa);
            responder(exchange, 201, multa);
        } else if ("PUT".equals(metodo) && id != null) {
            JsonObject datos = leerJson(exchange);
            MultaDTO multa = leerMulta(datos);
            multa.setIdMulta(parseId(id));
            if (multas.actualizar(multa)) {
                responder(exchange, 200, multa);
            } else {
                responderError(exchange, 404, "No se encontró la multa.");
            }
        } else if ("DELETE".equals(metodo) && id != null) {
            responderEliminacion(exchange, multas.eliminar(parseId(id)), "No se encontró la multa.");
        } else {
            metodoNoPermitido(exchange);
        }
    }

    private VehiculoDTO leerVehiculo(JsonObject datos) throws SQLException {
        int propietarioId = parseId(texto(datos, "idPropietario"));
        Optional<PropietarioDTO> propietario = propietarios.buscarPorId(propietarioId);
        if (propietario.isEmpty()) {
            throw new IllegalArgumentException("El propietario seleccionado no existe.");
        }
        VehiculoDTO vehiculo = new VehiculoDTO();
        vehiculo.setPlaca(texto(datos, "placa"));
        vehiculo.setMarca(texto(datos, "marca"));
        vehiculo.setModelo(texto(datos, "modelo"));
        vehiculo.setAño(numeroEntero(datos, "año"));
        vehiculo.setIdPropietario(propietarioId);
        return vehiculo;
    }

    private MultaDTO leerMulta(JsonObject datos) throws SQLException {
        int idVehiculo = parseId(texto(datos, "idVehiculo"));
        if (vehiculos.buscarPorId(idVehiculo).isEmpty()) {
            throw new IllegalArgumentException("El vehículo seleccionado no existe.");
        }
        MultaDTO multa = new MultaDTO();
        multa.setCodigoInfraccion(texto(datos, "codigoInfraccion"));
        multa.setDescripcion(texto(datos, "descripcion"));
        multa.setValor(numero(datos, "valor"));
        String fecha = texto(datos, "fecha");
        multa.setFecha(Date.valueOf(fecha));
        multa.setIdVehiculo(idVehiculo);
        return multa;
    }

    private PropietarioDTO leerPropietario(JsonObject datos) {
        PropietarioDTO propietario = new PropietarioDTO();
        propietario.setCedula(texto(datos, "cedula"));
        propietario.setNombres(texto(datos, "nombres"));
        propietario.setApellidos(texto(datos, "apellidos"));
        propietario.setTelefono(textoOpcional(datos, "telefono"));
        propietario.setEmail(textoOpcional(datos, "email"));
        String fecha = textoOpcional(datos, "fecha");
        propietario.setFecha(fecha == null ? null : Date.valueOf(fecha));
        return propietario;
    }

    private String textoOpcional(JsonObject datos, String campo) {
        if (!datos.has(campo) || datos.get(campo).isJsonNull()) {
            return null;
        }
        String valor = datos.get(campo).getAsString().trim();
        return valor.isBlank() ? null : valor;
    }

    private int parseId(String valor) {
        try {
            int id = Integer.parseInt(valor);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El identificador debe ser un entero positivo.");
        }
    }

    private int numeroEntero(JsonObject datos, String campo) {
        try {
            return datos.get(campo).getAsInt();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un entero.");
        }
    }

    private JsonObject leerJson(HttpExchange exchange) throws IOException {
        byte[] cuerpo = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (cuerpo.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("La solicitud es demasiado grande.");
        }
        var json = JsonParser.parseString(new String(cuerpo, StandardCharsets.UTF_8));
        if (!json.isJsonObject()) {
            throw new IllegalArgumentException("El cuerpo de la solicitud debe ser un objeto JSON.");
        }
        return json.getAsJsonObject();
    }

    private String texto(JsonObject datos, String campo) {
        if (!datos.has(campo) || datos.get(campo).isJsonNull()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        String valor = datos.get(campo).getAsString().trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return valor;
    }

    private double numero(JsonObject datos, String campo) {
        try {
            double valor = datos.get(campo).getAsDouble();
            if (!Double.isFinite(valor)) {
                throw new NumberFormatException();
            }
            return valor;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser numérico.");
        }
    }

    private void responderEliminacion(HttpExchange exchange, boolean eliminado, String error)
            throws IOException {
        if (eliminado) {
            responder(exchange, 204, null);
        } else {
            responderError(exchange, 404, error);
        }
    }

    private void metodoNoPermitido(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Allow", "GET, POST, PUT, DELETE");
        responderError(exchange, 405, "Método no permitido para esta ruta.");
    }

    private void responderError(HttpExchange exchange, int status, String mensaje)
            throws IOException {
        JsonObject respuesta = new JsonObject();
        respuesta.addProperty("error", mensaje == null ? "Solicitud inválida." : mensaje);
        responder(exchange, status, respuesta);
    }

    private void responder(HttpExchange exchange, int status, Object cuerpo) throws IOException {
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
            return;
        }
        byte[] contenido = GSON.toJson(cuerpo).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, contenido.length);
        exchange.getResponseBody().write(contenido);
        exchange.close();
    }

    private static final class SqlDateAdapter
            implements JsonSerializer<Date>, JsonDeserializer<Date> {
        @Override
        public com.google.gson.JsonElement serialize(
                Date value, java.lang.reflect.Type type,
                com.google.gson.JsonSerializationContext context) {
            return value == null ? null : new com.google.gson.JsonPrimitive(value.toString());
        }

        @Override
        public Date deserialize(
                com.google.gson.JsonElement value, java.lang.reflect.Type type,
                com.google.gson.JsonDeserializationContext context) {
            return value == null || value.isJsonNull() ? null : Date.valueOf(value.getAsString());
        }
    }
}
