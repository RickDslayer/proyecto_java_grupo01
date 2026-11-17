package com.carmotors.servicio;

import com.carmotors.controlador.AsignacionTecnicoController;
import com.carmotors.dao.ServicioTecnicoDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ServicioTecnico;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AsignacionTecnicoServiceTest {
    private final Memoria dao = new Memoria();
    private int aperturas, commits, rollbacks, cierres;
    private boolean fallaCommit;
    private final AsignacionTecnicoService servicio = new AsignacionTecnicoService(dao, () -> {
        aperturas++;
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, metodo, args) -> {
                    switch (metodo.getName()) {
                        case "setAutoCommit": assertEquals(false, args[0]); return null;
                        case "commit": if (fallaCommit) throw new SQLException("Error de commit"); commits++; return null;
                        case "rollback": rollbacks++; return null;
                        case "close": cierres++; return null;
                        default: throw new AssertionError(metodo.getName());
                    }
                });
    });

    @Test void variosTecnicosPorOrdenYVariasOrdenesPorTecnico() {
        servicio.asignar(1, 1); servicio.asignar(1, 2); servicio.asignar(2, 1);
        assertEquals(2, servicio.listarPorOrden(1).size());
        assertEquals(2, servicio.listarPorTecnico(1).size());
        assertEquals(List.of("orden:1", "tecnico:1", "orden:1", "tecnico:2", "orden:2", "tecnico:1"), dao.bloqueos);
        assertEquals(3, commits);
        assertEquals(5, cierres);
    }

    @Test void asignacionDuplicadaNoSeRepite() {
        servicio.asignar(1, 1);
        assertThrows(ValidacionException.class, () -> servicio.asignar(1, 1));
        assertEquals(1, servicio.listarPorOrden(1).size());
        assertEquals(1, commits); assertEquals(1, rollbacks);
    }

    @Test void referenciasInexistentesEInactivoSeRechazan() {
        assertThrows(ValidacionException.class, () -> servicio.asignar(99, 1));
        assertThrows(ValidacionException.class, () -> servicio.asignar(1, 99));
        dao.activos.put(2, false);
        assertThrows(ValidacionException.class, () -> servicio.asignar(1, 2));
        assertTrue(dao.filas.isEmpty()); assertEquals(3, rollbacks);
    }

    @Test void ordenesCerradasRechazanTodasLasModificaciones() {
        servicio.asignar(1, 1);
        for (String estado : List.of("Completado", "Facturado", "Entregado")) {
            dao.estados.put(1, estado);
            assertThrows(ValidacionException.class, () -> servicio.asignar(1, 2));
            assertThrows(ValidacionException.class, () -> servicio.quitar(1, 1));
            assertThrows(ValidacionException.class, () -> servicio.registrarHoras(1, 1, BigDecimal.ONE));
        }
        assertEquals(1, dao.filas.size()); assertEquals(9, rollbacks);
    }

    @Test void horasExactasNoSeAcumulanYSePermiteQuitarInactivo() {
        servicio.asignar(1, 1);
        servicio.registrarHoras(1, 1, new BigDecimal("2.50"));
        servicio.registrarHoras(1, 1, new BigDecimal("2.50"));
        assertEquals(new BigDecimal("2.50"), servicio.listarPorOrden(1).get(0).horasTrabajadas());
        dao.activos.put(1, false);
        servicio.quitar(1, 1);
        assertTrue(servicio.listarPorOrden(1).isEmpty());
        assertThrows(ValidacionException.class, () -> servicio.quitar(1, 1));
        assertThrows(ValidacionException.class, () -> servicio.registrarHoras(1, 1, BigDecimal.ONE));
    }

    @Test void idsYHorasInvalidasNoAbrenConexion() {
        assertThrows(ValidacionException.class, () -> servicio.asignar(0, 1));
        assertThrows(ValidacionException.class, () -> servicio.listarPorTecnico(-1));
        for (BigDecimal horas : List.of(new BigDecimal("-1"), new BigDecimal("1000"), new BigDecimal("0.001"))) {
            assertThrows(ValidacionException.class, () -> servicio.registrarHoras(1, 1, horas));
        }
        assertThrows(ValidacionException.class, () -> servicio.registrarHoras(1, 1, null));
        assertEquals(0, aperturas);
    }

    @Test void errorSqlYFalloCommitRevierteYCierra() {
        dao.error = new DaoException("PK duplicada", new SQLException("PK", "23000", 1062));
        assertThrows(ValidacionException.class, () -> servicio.asignar(1, 1));
        dao.error = new DaoException("Otro fallo");
        assertThrows(DaoException.class, () -> servicio.asignar(1, 1));
        dao.error = null; fallaCommit = true;
        assertThrows(DaoException.class, () -> servicio.asignar(1, 1));
        assertEquals(3, rollbacks); assertEquals(0, commits); assertEquals(3, cierres);
    }

    @Test void controladorAdaptaEntradasYMensajes() {
        AsignacionTecnicoController controller = new AsignacionTecnicoController(servicio);
        assertTrue(controller.asignar("a", "1").startsWith("Error:"));
        assertTrue(controller.asignar("1", "1").contains("asignado"));
        assertTrue(controller.asignar("1", "1").contains("ya está asignado"));
        assertTrue(controller.registrarHoras("1", "1", "3.25").contains("actualizadas"));
        assertTrue(controller.registrarHoras("1", "1", null).startsWith("Error:"));
        assertTrue(controller.listarPorOrden("1").contains("3.25"));
        assertTrue(controller.listarPorTecnico("1").contains("Orden #1"));
        assertTrue(controller.quitar("1", "1").contains("retirada"));
        assertTrue(controller.listarPorOrden("1").contains("No hay asignaciones"));
    }

    private static class Memoria implements ServicioTecnicoDAO {
        final Map<Integer,String> estados = new HashMap<>(Map.of(1,"Pendiente",2,"En Proceso"));
        final Map<Integer,Boolean> activos = new HashMap<>(Map.of(1,true,2,true));
        final Map<String,ServicioTecnico> filas = new HashMap<>();
        final List<String> bloqueos = new ArrayList<>();
        DaoException error;
        public Optional<String> bloquearEstadoOrden(Connection c, int id) { bloqueos.add("orden:"+id); return Optional.ofNullable(estados.get(id)); }
        public Optional<Boolean> bloquearTecnicoActivo(Connection c, int id) { bloqueos.add("tecnico:"+id); return Optional.ofNullable(activos.get(id)); }
        public boolean existe(Connection c, int orden, int tecnico) { return filas.containsKey(orden+":"+tecnico); }
        public void asignar(Connection c, int orden, int tecnico) {
            if (error != null) throw error;
            filas.put(orden+":"+tecnico, new ServicioTecnico(orden,tecnico,"Técnico "+tecnico,estados.get(orden),new BigDecimal("0.00")));
        }
        public boolean quitar(Connection c, int orden, int tecnico) { return filas.remove(orden+":"+tecnico) != null; }
        public boolean actualizarHoras(Connection c, int orden, int tecnico, BigDecimal horas) {
            ServicioTecnico f=filas.get(orden+":"+tecnico);
            boolean cambio=f.horasTrabajadas().compareTo(horas)!=0;
            filas.put(orden+":"+tecnico,new ServicioTecnico(orden,tecnico,f.nombreTecnico(),f.estadoOrden(),horas)); return cambio;
        }
        public List<ServicioTecnico> listarPorOrden(Connection c, int orden) { return filas.values().stream().filter(f->f.servicioId()==orden).toList(); }
        public List<ServicioTecnico> listarPorTecnico(Connection c, int tecnico) { return filas.values().stream().filter(f->f.tecnicoId()==tecnico).toList(); }
    }
}
