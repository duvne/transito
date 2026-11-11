# Sistema de tránsito

## Ejecución sin Docker

Se requiere Java 21 y Maven. Configura `TRANSITO_DB_URL`, `TRANSITO_DB_USER` y
`TRANSITO_DB_PASSWORD` para una instancia PostgreSQL activa y ejecuta:

```powershell
mvn compile exec:java
```

## Capas

- `presentacion.ServidorWebTransito`: servidor HTTP local para la interfaz web y la API.
- `src/main/resources/index.html`: interfaz HTML.
- `servicio`: validaciones y lógica de negocio.
- `persistencia`: interfaces DAO e implementaciones JDBC.
- `entidad`: DTOs y relaciones entre propietario, vehículo y multa.
