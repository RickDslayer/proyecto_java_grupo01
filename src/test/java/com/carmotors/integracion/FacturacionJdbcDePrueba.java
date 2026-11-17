package com.carmotors.integracion;

import com.carmotors.excepcion.DaoException;
import com.carmotors.servicio.FacturacionOrden;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** DOBLE EXCLUSIVO DE PRUEBAS: persiste totales simples, sin impuestos ni TXT. */
final class FacturacionJdbcDePrueba implements FacturacionOrden {
    private boolean fallarTrasInsertar;
    void setFallarTrasInsertar(boolean fallar) { fallarTrasInsertar=fallar; }

    @Override public void facturar(Connection con,int orden,LocalDateTime fecha) {
        BigDecimal repuestos=total(con,"SELECT COALESCE(SUM(cantidad_requerida*precio_unitario_venta),0) FROM servicios_repuestos WHERE servicio_id=?",orden);
        BigDecimal manoObra=total(con,"SELECT COALESCE(SUM(precio_cobrado),0) FROM servicios_detalle WHERE servicio_id=?",orden);
        String sql="INSERT INTO facturas(servicio_id,fecha_emision,total_repuestos,total_mano_obra,impuestos,total_final) VALUES(?,?,?,?,0,?)";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,orden);ps.setTimestamp(2,Timestamp.valueOf(fecha));ps.setBigDecimal(3,repuestos);
            ps.setBigDecimal(4,manoObra);ps.setBigDecimal(5,repuestos.add(manoObra));ps.executeUpdate();
            if(fallarTrasInsertar) throw new DaoException("Fallo de prueba tras insertar factura; debe revertirse.");
        }catch(SQLException e){throw new DaoException("Falló el doble de facturación.",e);}
    }
    private BigDecimal total(Connection con,String sql,int orden){
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,orden);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getBigDecimal(1);}
        }catch(SQLException e){throw new DaoException("No se pudo calcular un total de prueba.",e);}
    }
}
