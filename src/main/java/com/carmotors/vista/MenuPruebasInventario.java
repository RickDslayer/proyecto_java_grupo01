package com.carmotors.vista;

import com.carmotors.modelo.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Scanner;

/** Menú de consola para demostraciones manuales del Issue #35. Solo permite escrituras en carmotors_test. */
public final class MenuPruebasInventario {
    private final Scanner entrada = new Scanner(System.in);
    private final ContextoInventario c = new ContextoInventario();

    public static void main(String[] args) {
        new MenuPruebasInventario().iniciar();
    }
    private void iniciar() {
        System.out.println("\n=== CarMotors | Pruebas de inventario (Issue #35) ===");
        System.out.println("Base detectada: " + c.baseActual());
        System.out.println("Las escrituras se permiten SOLO en carmotors_test.");
        while (true) {
            System.out.println("\n1. CRUD repuestos");
            System.out.println("2. Ingreso y consulta de lotes");
            System.out.println("3. Existencias y reservas");
            System.out.println("4. Alertas Observer");
            System.out.println("5. Validaciones y rollback");
            System.out.println("6. Consistencia de data.sql");
            System.out.println("7. CRUD tipos de repuesto");
            System.out.println("0. Salir");
            switch (leer("Opción")) {
                case "1" -> ejecutar(this::repuestos);
                case "7" -> ejecutar(this::tiposRepuesto);
                case "2" -> ejecutar(this::lotes);
                case "3" -> ejecutar(this::existencias);
                case "4" -> ejecutar(this::alertas);
                case "5" -> ejecutar(this::validaciones);
                case "6" -> ejecutar(this::consistencia);
                case "0" -> { System.out.println("Fin de las pruebas."); return; }
                default -> System.out.println("Opción no válida.");
            }
        }
    }
    private void ejecutar(Runnable r) {
        try { r.run(); }
        catch (RuntimeException e) { System.out.println("ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
    }
    private String leer(String titulo) {
        System.out.print(titulo + ": ");
        return entrada.nextLine().trim();
    }
    private String leerConDefecto(String titulo, String defecto) {
        String dato = leer(titulo + " [" + defecto + "]");
        return dato.isBlank() ? defecto : dato;
    }
    private boolean escrituraPermitida() {
        if (!c.basePruebasValida()) {
            System.out.println("BLOQUEADO: se requiere conexión a la base carmotors_test.");
            return false;
        }
        return "SI".equalsIgnoreCase(leer("Modificará datos de PRUEBA. Escribe SI para continuar"));
    }
    private String[] datosRepuesto() {
        System.out.println("Tipos de repuesto disponibles:");
        System.out.println(c.tiposCtrl.listar());
        return new String[]{leer("Nombre"),leer("ID tipo"),leer("ID marca"),leer("ID proveedor"),
                leer("Precio de compra"),leer("Precio de venta"),leer("Stock mínimo")};
    }
    private void repuestos() {
        System.out.println("\n1. Listar  2. Buscar por ID  3. Buscar por nombre  4. Crear  5. Actualizar  6. Eliminar  7. Filtrar por tipo");
        String op=leer("Acción");
        switch(op) {
            case "1" -> System.out.println(c.repuestosCtrl.listar());
            case "2" -> System.out.println(c.repuestosCtrl.buscarPorId(leer("ID")));
            case "3" -> System.out.println(c.repuestosCtrl.buscarPorNombre(leer("Nombre")));
            case "4" -> { if(!escrituraPermitida())return; String[] a=datosRepuesto(); System.out.println(c.repuestosCtrl.crear(a[0],a[1],a[2],a[3],a[4],a[5],a[6])); }
            case "5" -> {if(!escrituraPermitida())return;String id=leer("ID a modificar");String[] a=datosRepuesto();System.out.println(c.repuestosCtrl.actualizar(id,a[0],a[1],a[2],a[3],a[4],a[5],a[6]));}
            case "6" -> {if(escrituraPermitida())System.out.println(c.repuestosCtrl.eliminar(leer("ID a eliminar")));}
            case "7" -> System.out.println(c.repuestosCtrl.listarPorTipo(leer("ID tipo")));
            default -> System.out.println("Opción no válida.");
        }
    }
    /** CRUD de tipos independiente del CRUD de repuestos. */
    private void tiposRepuesto() {
        System.out.println("\n--- Tipos de repuesto ---");
        System.out.println("1. Listar  2. Buscar por ID  3. Buscar por nombre  4. Crear  5. Actualizar  6. Eliminar  7. Ver repuestos de un tipo");
        switch (leer("Acción")) {
            case "1" -> System.out.println(c.tiposCtrl.listar());
            case "2" -> System.out.println(c.tiposCtrl.buscarPorId(leer("ID tipo")));
            case "3" -> System.out.println(c.tiposCtrl.buscarPorNombre(leer("Nombre o parte del nombre")));
            case "4" -> {
                if (!escrituraPermitida()) return;
                System.out.println(c.tiposCtrl.crear(leer("Nombre del nuevo tipo")));
            }
            case "5" -> {
                if (!escrituraPermitida()) return;
                System.out.println(c.tiposCtrl.actualizar(leer("ID del tipo"), leer("Nombre nuevo")));
            }
            case "6" -> {
                if (!escrituraPermitida()) return;
                System.out.println("AVISO: no es posible eliminar un tipo con repuestos asociados.");
                System.out.println(c.tiposCtrl.eliminar(leer("ID del tipo")));
            }
            case "7" -> System.out.println(c.repuestosCtrl.listarPorTipo(leer("ID tipo")));
            default -> System.out.println("Opción no válida.");
        }
    }

    private void lotes() {
        System.out.println("\n1. Buscar por código  2. Lotes de un repuesto  3. Stock simple  4. FEFO  5. Registrar ingreso");
        switch(leer("Acción")) {
            case "1" -> System.out.println(c.lotesCtrl.buscarPorCodigo(leer("Código")));
            case "2" -> System.out.println(c.lotesCtrl.listarPorRepuesto(leer("ID repuesto")));
            case "3" -> System.out.println(c.lotesCtrl.consultarStock(leer("ID repuesto")));
            case "4" -> System.out.println(c.lotesCtrl.seleccionarFefo(leer("ID repuesto"),leer("Cantidad solicitada")));
            case "5" -> {
                if(!escrituraPermitida())return;
                String codigo=leer("Código único"), proveedor=leer("ID proveedor"), fecha=leerConDefecto("Fecha ingreso AAAA-MM-DD",LocalDate.now().toString());
                String repuesto=leer("ID repuesto"), cantidad=leer("Cantidad"), costo=leer("Costo unitario"),caducidad=leer("Fecha caducidad AAAA-MM-DD (opcional)");
                System.out.println(c.lotesCtrl.registrarIngreso(codigo,proveedor,fecha,repuesto,cantidad,costo,caducidad));
                System.out.println("Eventos Observer recibidos: " + c.eventos);
            }
            default -> System.out.println("Opción no válida.");
        }
    }
    private void existencias() {
        int id=Integer.parseInt(leer("ID repuesto"));
        System.out.println("\n1. Resumen  2. Ver lotes  3. Buscar lote específico");
        switch(leer("Acción")) {
            case "1" -> {StockRepuesto s=c.existencias.resumen(id);
                System.out.printf("Físico: %d | Reservado: %d | Disponible: %d | Utilizable: %d%n",s.fisico(),s.reservadoActivo(),s.disponible(),s.disponibleUtilizable());}
            case "2" -> {var lotes=c.existencias.porRepuesto(id);if(lotes.isEmpty())System.out.println("Sin lotes.");for(StockLote l:lotes)System.out.println(l);}
            case "3" -> System.out.println(c.existencias.porLote(Integer.parseInt(leer("ID lote")),id).map(Object::toString).orElse("No existe esa pareja lote/repuesto."));
            default -> System.out.println("Opción no válida.");
        }
    }
    private void alertas() {
        System.out.println("\n1. Consultar alertas  2. Últimas alertas  3. Eventos recibidos");
        switch(leer("Acción")) {
            case "1" -> System.out.println(c.alertasCtrl.mostrarReporte());
            case "2" -> c.alertas.ultimasAlertas().forEach(System.out::println);
            case "3" -> System.out.println("Repuestos notificados en esta ejecución: " + c.eventos);
            default -> System.out.println("Opción no válida.");
        }
    }
    private void verificarRechazo(Runnable accion) {
        try { accion.run(); System.out.println("FALLO: se aceptó el dato inválido."); }
        catch (RuntimeException ex) { System.out.println("OK: rechazado con " + ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
    }
    private void validaciones() {
        System.out.println("\n1. Físico negativo  2. Reserva superior al físico  3. Ingreso negativo  4. Rollback SQL + Observer");
        switch(leer("Acción")) {
            case "1" -> verificarRechazo(()->new StockLote(1,"INV",1,null,-1,0));
            case "2" -> verificarRechazo(()->new StockLote(1,"INV",1,null,2,3));
            case "3" -> verificarRechazo(()->{
                Lote lote=new Lote();lote.setCodigo("INVALIDO-NEG");lote.setProveedorId(1);lote.setFechaIngreso(LocalDate.now());
                LoteLinea ln=new LoteLinea();ln.setRepuestoId(1);ln.setCantidadIngresada(-5);ln.setCostoUnitario(BigDecimal.ONE);lote.agregarLinea(ln);c.lotes.registrarIngreso(lote);
            });
            case "4" -> {
                if(!escrituraPermitida())return;
                int repuestoId=Integer.parseInt(leer("ID de repuesto EXISTENTE"));
                String codigo="ROLLBACK-DEMO-"+System.currentTimeMillis();
                Lote lote=new Lote();lote.setCodigo(codigo);lote.setProveedorId(1);lote.setFechaIngreso(LocalDate.now());
                LoteLinea bien=new LoteLinea();bien.setRepuestoId(repuestoId);bien.setCantidadIngresada(2);bien.setCostoUnitario(BigDecimal.ONE);
                LoteLinea mal=new LoteLinea();mal.setRepuestoId(Integer.MAX_VALUE);mal.setCantidadIngresada(2);mal.setCostoUnitario(BigDecimal.ONE);
                lote.agregarLinea(bien);lote.agregarLinea(mal);
                int anteriores=c.eventos.size();
                boolean fallo=false;
                try { c.lotes.registrarIngreso(lote); } catch(RuntimeException ex) { fallo=true;System.out.println("Error SQL esperado: "+ex.getMessage()); }
                boolean persistio=!c.lotes.buscarPorCodigo(codigo).isEmpty();
                boolean notifico=c.eventos.size()>anteriores;
                System.out.printf("Error ocurrido: %s | Lote parcial: %s | Observer emitió evento: %s%n",fallo,persistio,notifico);
                System.out.println(fallo&&!persistio&&!notifico?"OK: rollback consistente.":"REVISAR: no se cumplió el comportamiento esperado.");
            }
            default -> System.out.println("Opción no válida.");
        }
    }
    private void consistencia() {
        System.out.println("Base actual: "+c.baseActual());
        if (!c.basePruebasValida()) {System.out.println("Se requiere carmotors_test para validar los datos de prueba.");return;}
        String[] tablas={"repuestos","lotes","lotes_repuestos","reservas_repuestos","proveedores"};
        int[] esperados={12,6,15,18,4};
        try(Connection con=c.conexiones.get();Statement st=con.createStatement()){
            for(int i=0;i<tablas.length;i++){
                try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM "+tablas[i])){
                    rs.next();int n=rs.getInt(1);
                    System.out.printf("%-20s actual %d | data.sql %d %s%n",tablas[i],n,esperados[i],n==esperados[i]?"[OK]":"[DIFERENTE]");
                }
            }
            try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM lotes_repuestos WHERE cantidad_actual > cantidad_ingresada OR cantidad_actual < 0 OR cantidad_ingresada <= 0")){
                rs.next();System.out.println("Líneas inconsistentes: "+rs.getLong(1));
            }
            try(ResultSet rs=st.executeQuery("SELECT COUNT(*) FROM (SELECT lr.lote_id,lr.repuesto_id FROM lotes_repuestos lr JOIN reservas_repuestos rr ON rr.lote_id=lr.lote_id AND rr.repuesto_id=lr.repuesto_id JOIN reserva_estados re ON re.id=rr.estado_id AND re.nombre='ACTIVA' GROUP BY lr.lote_id,lr.repuesto_id,lr.cantidad_actual HAVING SUM(rr.cantidad)>lr.cantidad_actual) t")){
                rs.next();System.out.println("Lotes con reservas activas mayores al físico: "+rs.getLong(1));
            }
            System.out.println("Nota: conteos esperados para data.sql recién cargado; después de registrar lotes pueden diferir.");
        }catch(SQLException ex){System.out.println("Error SQL: "+ex.getMessage());}
    }
}
