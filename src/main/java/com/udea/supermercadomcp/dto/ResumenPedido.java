package com.udea.supermercadomcp.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenPedido(Long pedidoId, Long clienteId, String estado, String fecha,
                            String direccionEntrega, List<LineaPedido> lineas, BigDecimal total) {}
