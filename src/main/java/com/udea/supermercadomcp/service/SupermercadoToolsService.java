package com.udea.supermercadomcp.service;

import com.udea.supermercadomcp.dto.LineaPedido;
import com.udea.supermercadomcp.dto.ResumenPedido;
import com.udea.supermercadomcp.model.Cliente;
import com.udea.supermercadomcp.model.DetallePedido;
import com.udea.supermercadomcp.model.Pedido;
import com.udea.supermercadomcp.model.Producto;
import com.udea.supermercadomcp.repository.ClienteRepository;
import com.udea.supermercadomcp.repository.DetallePedidoRepository;
import com.udea.supermercadomcp.repository.PedidoRepository;
import com.udea.supermercadomcp.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Herramientas MCP para pedidos de supermercado a domicilio.
 * Flujo: buscar productos -> registrar/buscar cliente -> crear pedido (BORRADOR)
 *        -> agregar productos -> confirmar (descuenta stock) / cancelar.
 * Todas las reglas de negocio se validan aquí, en el servidor, no en el modelo de IA.
 */
@Component
public class SupermercadoToolsService {

    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;

    public SupermercadoToolsService(ProductoRepository productos, ClienteRepository clientes,
                                    PedidoRepository pedidos, DetallePedidoRepository detalles) {
        this.productos = productos;
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.detalles = detalles;
    }

    // ===================== PRODUCTOS =====================

    @Tool(description = "Lista todos los productos del supermercado con id, categoría, precio en pesos y stock disponible.")
    public List<Producto> listarProductos() {
        return productos.findAll();
    }

    @Tool(description = "Busca productos por nombre o categoría usando un fragmento de texto, sin distinguir mayúsculas.")
    public List<Producto> buscarProductos(
            @ToolParam(description = "Texto a buscar, por ejemplo 'leche', 'aseo' o 'arroz'") String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Indique un texto de búsqueda");
        }
        String t = texto.trim();
        return productos.findByNombreContainingIgnoreCaseOrCategoriaContainingIgnoreCase(t, t);
    }

    @Tool(description = "Consulta el precio y el stock disponible de un producto por su id.")
    public Producto consultarStock(
            @ToolParam(description = "Id del producto") Long productoId) {
        return productoPorId(productoId);
    }

    // ===================== CLIENTES =====================

    @Tool(description = "Registra un cliente con nombre, teléfono y dirección de entrega. Si el teléfono ya existe, devuelve el cliente existente.")
    @Transactional
    public Cliente registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Teléfono del cliente (identificador único)") String telefono,
            @ToolParam(description = "Dirección de entrega del cliente") String direccion) {
        if (nombre == null || nombre.isBlank() || telefono == null || telefono.isBlank()
                || direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("Nombre, teléfono y dirección son obligatorios");
        }
        return clientes.findByTelefono(telefono.trim())
                .orElseGet(() -> clientes.save(new Cliente(nombre.trim(), telefono.trim(), direccion.trim())));
    }

    @Tool(description = "Busca un cliente registrado por su número de teléfono.")
    public Cliente buscarCliente(
            @ToolParam(description = "Teléfono del cliente") String telefono) {
        if (telefono == null || telefono.isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
        }
        return clientes.findByTelefono(telefono.trim())
                .orElseThrow(() -> new IllegalArgumentException("No existe un cliente con el teléfono " + telefono));
    }

    // ===================== PEDIDOS =====================

    @Tool(description = "Crea un pedido vacío en estado BORRADOR para un cliente. Si no se indica dirección, usa la del cliente.")
    @Transactional
    public ResumenPedido crearPedido(
            @ToolParam(description = "Id del cliente") Long clienteId,
            @ToolParam(description = "Dirección de entrega del pedido (opcional)", required = false) String direccionEntrega) {
        Cliente c = clientes.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cliente " + clienteId));
        String dir = (direccionEntrega == null || direccionEntrega.isBlank())
                ? c.getDireccion() : direccionEntrega.trim();
        Pedido p = pedidos.save(new Pedido(c.getId(), LocalDateTime.now().toString(),
                Pedido.BORRADOR, dir, BigDecimal.ZERO));
        return resumen(p, List.of());
    }

    @Tool(description = "Agrega un producto con cantidad a un pedido en BORRADOR. Valida que haya stock suficiente. Si el producto ya estaba, suma la cantidad.")
    @Transactional
    public ResumenPedido agregarProductoAlPedido(
            @ToolParam(description = "Id del pedido") Long pedidoId,
            @ToolParam(description = "Id del producto") Long productoId,
            @ToolParam(description = "Cantidad a agregar (entero mayor que cero)") Integer cantidad) {
        Pedido p = pedidoBorrador(pedidoId);
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        Producto prod = productoPorId(productoId);
        DetallePedido linea = detalles.findByPedidoIdAndProductoId(pedidoId, productoId).orElse(null);
        int nueva = cantidad + (linea == null ? 0 : linea.getCantidad());
        if (prod.getStock() < nueva) {
            throw new IllegalArgumentException("Stock insuficiente de " + prod.getNombre()
                    + ": disponible " + prod.getStock() + ", solicitado " + nueva);
        }
        if (linea == null) {
            linea = new DetallePedido(pedidoId, prod.getId(), prod.getNombre(), cantidad, prod.getPrecio());
        } else {
            linea.setCantidad(nueva);
        }
        detalles.save(linea);
        return recalcular(p);
    }

    @Tool(description = "Quita un producto de un pedido en BORRADOR.")
    @Transactional
    public ResumenPedido quitarProductoDelPedido(
            @ToolParam(description = "Id del pedido") Long pedidoId,
            @ToolParam(description = "Id del producto a quitar") Long productoId) {
        Pedido p = pedidoBorrador(pedidoId);
        DetallePedido linea = detalles.findByPedidoIdAndProductoId(pedidoId, productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto " + productoId + " no está en el pedido " + pedidoId));
        detalles.delete(linea);
        return recalcular(p);
    }

    @Tool(description = "Confirma un pedido en BORRADOR: verifica el stock, lo descuenta y deja el pedido en CONFIRMADO. Pedir confirmación al cliente antes de usarla.")
    @Transactional
    public ResumenPedido confirmarPedido(
            @ToolParam(description = "Id del pedido a confirmar") Long pedidoId) {
        Pedido p = pedidoBorrador(pedidoId);
        List<DetallePedido> lineas = detalles.findByPedidoId(pedidoId);
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " no tiene productos");
        }
        for (DetallePedido l : lineas) {            // 1) validar todo antes de modificar
            Producto prod = productoPorId(l.getProductoId());
            if (prod.getStock() < l.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente de " + prod.getNombre()
                        + ": disponible " + prod.getStock() + ", en el pedido " + l.getCantidad());
            }
        }
        for (DetallePedido l : lineas) {            // 2) descontar stock
            Producto prod = productoPorId(l.getProductoId());
            prod.setStock(prod.getStock() - l.getCantidad());
            productos.save(prod);
        }
        p.setEstado(Pedido.CONFIRMADO);
        return recalcular(p);
    }

    @Tool(description = "Consulta un pedido con sus productos, estado y total.")
    public ResumenPedido consultarPedido(
            @ToolParam(description = "Id del pedido") Long pedidoId) {
        Pedido p = pedidoPorId(pedidoId);
        return resumen(p, detalles.findByPedidoId(pedidoId));
    }

    @Tool(description = "Lista los pedidos de un cliente, del más reciente al más antiguo.")
    public List<ResumenPedido> listarPedidosCliente(
            @ToolParam(description = "Id del cliente") Long clienteId) {
        return pedidos.findByClienteIdOrderByIdDesc(clienteId).stream()
                .map(p -> resumen(p, detalles.findByPedidoId(p.getId())))
                .toList();
    }

    @Tool(description = "Cancela un pedido (BORRADOR o CONFIRMADO). Si estaba confirmado, devuelve el stock. Pedir confirmación al cliente antes de usarla.")
    @Transactional
    public ResumenPedido cancelarPedido(
            @ToolParam(description = "Id del pedido a cancelar") Long pedidoId) {
        Pedido p = pedidoPorId(pedidoId);
        if (Pedido.CANCELADO.equals(p.getEstado())) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " ya está cancelado");
        }
        List<DetallePedido> lineas = detalles.findByPedidoId(pedidoId);
        if (Pedido.CONFIRMADO.equals(p.getEstado())) {
            for (DetallePedido l : lineas) {
                Producto prod = productoPorId(l.getProductoId());
                prod.setStock(prod.getStock() + l.getCantidad());
                productos.save(prod);
            }
        }
        p.setEstado(Pedido.CANCELADO);
        pedidos.save(p);
        return resumen(p, lineas);
    }

    // ===================== AUXILIARES =====================

    private Producto productoPorId(Long id) {
        if (id == null) throw new IllegalArgumentException("El id del producto es obligatorio");
        return productos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + id));
    }

    private Pedido pedidoPorId(Long id) {
        if (id == null) throw new IllegalArgumentException("El id del pedido es obligatorio");
        return pedidos.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + id));
    }

    private Pedido pedidoBorrador(Long id) {
        Pedido p = pedidoPorId(id);
        if (!Pedido.BORRADOR.equals(p.getEstado())) {
            throw new IllegalArgumentException("El pedido " + id + " está " + p.getEstado()
                    + " y ya no se puede modificar");
        }
        return p;
    }

    private ResumenPedido recalcular(Pedido p) {
        List<DetallePedido> lineas = detalles.findByPedidoId(p.getId());
        BigDecimal total = lineas.stream()
                .map(l -> l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        p.setTotal(total);
        pedidos.save(p);
        return resumen(p, lineas);
    }

    private ResumenPedido resumen(Pedido p, List<DetallePedido> lineas) {
        List<LineaPedido> ls = lineas.stream()
                .map(l -> new LineaPedido(l.getProductoId(), l.getNombreProducto(), l.getCantidad(),
                        l.getPrecioUnitario(),
                        l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad()))))
                .toList();
        return new ResumenPedido(p.getId(), p.getClienteId(), p.getEstado(), p.getFechaCreacion(),
                p.getDireccionEntrega(), ls, p.getTotal());
    }
}

