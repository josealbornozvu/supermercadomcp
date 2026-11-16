package com.udea.supermercadomcp.repository;

import com.udea.supermercadomcp.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteIdOrderByIdDesc(Long clienteId);
}

