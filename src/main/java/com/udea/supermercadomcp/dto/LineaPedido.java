package com.udea.supermercadomcp.dto;

import java.math.BigDecimal;

public record LineaPedido(Long productoId, String producto, Integer cantidad,
                          BigDecimal precioUnitario, BigDecimal subtotal) {}
