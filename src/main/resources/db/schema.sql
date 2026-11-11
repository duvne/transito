CREATE TABLE IF NOT EXISTS propietarios (
    id_propietario SERIAL PRIMARY KEY,
    cedula VARCHAR(50) NOT NULL UNIQUE,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(254),
    fecha DATE
);

CREATE TABLE IF NOT EXISTS vehiculos (
    id_vehiculo SERIAL PRIMARY KEY,
    placa VARCHAR(20) NOT NULL UNIQUE,
    marca VARCHAR(100) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    año INTEGER NOT NULL CHECK (año = 0 OR año >= 1886),
    propietario_id INTEGER NOT NULL,
    CONSTRAINT fk_vehiculo_propietario
        FOREIGN KEY (propietario_id) REFERENCES propietarios (id_propietario)
);

CREATE TABLE IF NOT EXISTS multas (
    id_multa SERIAL PRIMARY KEY,
    codigo_infraccion VARCHAR(50) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    valor NUMERIC(12, 2) NOT NULL CHECK (valor >= 0),
    fecha DATE NOT NULL,
    id_vehiculo INTEGER NOT NULL,
    CONSTRAINT fk_multa_vehiculo
        FOREIGN KEY (id_vehiculo) REFERENCES vehiculos (id_vehiculo)
);
