package com.carmotors.vista;

import com.carmotors.config.ConexionFactory;
import com.carmotors.controlador.*;
import com.carmotors.dao.factory.*;
import com.carmotors.servicio.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Servicios compartidos, conectados mediante un único publicador Observer. */
final class ContextoInventario {
    final Supplier<Connection> conexiones = () -> ConexionFactory.getInstancia().crearConexion();
    final Supplier<LocalDate> hoy = LocalDate::now;
    final PublicadorInventario publicador = new PublicadorInventario();
    final ProveedorConsulta proveedores = new ProveedorConsultaMemoria();
    final RepuestoTipoService tipos = new RepuestoTipoService(new RepuestoTipoDAOFactory().obtenerDAO(), conexiones);
    final RepuestoTipoController tiposCtrl = new RepuestoTipoController(tipos);
    final RepuestoService repuestos = new RepuestoService(new RepuestoDAOFactory(), proveedores, conexiones, publicador);
    final LoteService lotes = new LoteService(new LoteDAOFactory().obtenerDAO(), proveedores, conexiones, hoy, publicador);
    final ExistenciasService existencias = new ExistenciasService(new ExistenciasDAOFactory().obtenerDAO(), conexiones, hoy);
    final AlertasInventarioService alertas = new AlertasInventarioService(new AlertasDAOFactory().obtenerDAO(), existencias, conexiones, hoy, 7);
    final RepuestoController repuestosCtrl = new RepuestoController(repuestos);
    final LoteController lotesCtrl = new LoteController(lotes);
    final AlertasInventarioController alertasCtrl = new AlertasInventarioController(alertas);
    final List<Integer> eventos = new ArrayList<>();

    ContextoInventario() {
        publicador.suscribir(alertas);
        publicador.suscribir(eventos::add);
    }

    String baseActual() {
        try (Connection con = conexiones.get(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            return rs.next() ? rs.getString(1) : null;
        } catch (Exception e) {
            return null;
        }
    }
    boolean basePruebasValida() { return "carmotors_test".equalsIgnoreCase(baseActual()); }
}
