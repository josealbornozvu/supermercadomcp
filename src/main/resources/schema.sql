-- Ejecutar una sola vez:
-- & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 -e "source C:/RUTA/supermercado-mcp/src/main/resources/schema.sql"
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS supermercado_mcp
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE supermercado_mcp;

CREATE TABLE IF NOT EXISTS producto (
                                        id        BIGINT        NOT NULL AUTO_INCREMENT,
                                        nombre    VARCHAR(255)  NOT NULL,
    categoria VARCHAR(100),
    precio    DECIMAL(12,2) NOT NULL,
    stock     INT           NOT NULL DEFAULT 0,
    version   BIGINT        DEFAULT 0,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cliente (
                                       id        BIGINT       NOT NULL AUTO_INCREMENT,
                                       nombre    VARCHAR(255) NOT NULL,
    telefono  VARCHAR(30)  NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cliente_telefono (telefono)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pedido (
                                      id                BIGINT        NOT NULL AUTO_INCREMENT,
                                      cliente_id        BIGINT        NOT NULL,
                                      fecha_creacion    VARCHAR(40),
    estado            VARCHAR(20)   NOT NULL,
    direccion_entrega VARCHAR(255),
    total             DECIMAL(12,2) DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS detalle_pedido (
                                              id              BIGINT        NOT NULL AUTO_INCREMENT,
                                              pedido_id       BIGINT        NOT NULL,
                                              producto_id     BIGINT        NOT NULL,
                                              nombre_producto VARCHAR(255),
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (pedido_id)   REFERENCES pedido(id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO producto (nombre, categoria, precio, stock, version) VALUES
                                                                     ('Leche entera 1L',            'Lácteos',            4200,  50, 0),
                                                                     ('Queso campesino 500g',       'Lácteos',           12500,  30, 0),
                                                                     ('Yogurt natural 1L',          'Lácteos',            7800,  25, 0),
                                                                     ('Mantequilla 250g',           'Lácteos',            6500,  20, 0),
                                                                     ('Arroz blanco 1kg',           'Granos',             4800,  80, 0),
                                                                     ('Frijol rojo 500g',           'Granos',             5200,  40, 0),
                                                                     ('Lentejas 500g',              'Granos',             4300,  35, 0),
                                                                     ('Azúcar 1kg',                 'Granos',             4500,  60, 0),
                                                                     ('Sal 500g',                   'Granos',             1800,  70, 0),
                                                                     ('Pasta espagueti 500g',       'Granos',             3600,  45, 0),
                                                                     ('Aceite vegetal 1L',          'Granos',            12900,  30, 0),
                                                                     ('Banano 1kg',                 'Frutas y verduras',  3200,  40, 0),
                                                                     ('Tomate chonto 1kg',          'Frutas y verduras',  4500,  35, 0),
                                                                     ('Cebolla cabezona 1kg',       'Frutas y verduras',  3800,  40, 0),
                                                                     ('Papa pastusa 1kg',           'Frutas y verduras',  3400,  50, 0),
                                                                     ('Aguacate unidad',            'Frutas y verduras',  3500,  30, 0),
                                                                     ('Pechuga de pollo 1kg',       'Carnes',            17500,  25, 0),
                                                                     ('Carne molida 500g',          'Carnes',            14800,  20, 0),
                                                                     ('Huevos AA x30',              'Carnes',            17900,  30, 0),
                                                                     ('Agua 600ml',                 'Bebidas',            2200, 100, 0),
                                                                     ('Gaseosa 1.5L',               'Bebidas',            5900,  40, 0),
                                                                     ('Café molido 500g',           'Bebidas',           19800,  30, 0),
                                                                     ('Pan tajado 500g',            'Panadería',          6200,  30, 0),
                                                                     ('Detergente en polvo 1kg',    'Aseo',              14500,  25, 0),
                                                                     ('Jabón de baño x3',           'Aseo',               7900,  40, 0),
                                                                     ('Papel higiénico x12',        'Aseo',              21900,  30, 0);
