package com.udea.supermercadomcp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pedido")
public class Pedido {

    public static final String BORRADOR = "BORRADOR";
    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String CANCELADO = "CANCELADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;
    private String fechaCreacion;
    private String estado;
    private String direccionEntrega;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    public Pedido() {}

    public Pedido(Long clienteId, String fechaCreacion, String estado, String direccionEntrega, BigDecimal total) {
        this.clienteId = clienteId;
        this.fechaCreacion = fechaCreacion;
        this.estado = estado;
        this.direccionEntrega = direccionEntrega;
        this.total = total;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}

