package com.udea.supermercadomcp.repository;

import com.udea.supermercadomcp.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    List<DetallePedido> findByPedidoId(Long pedidoId);
    Optional<DetallePedido> findByPedidoIdAndProductoId(Long pedidoId, Long productoId);
}

