package com.carmotors.dao;

import com.carmotors.modelo.StockLote;
import java.sql.Connection;
import java.util.List;

/** Contrato de SOLO LECTURA. Nunca inserta reservas ni consume unidades. */
public interface ExistenciasDAO {
    /** Todas las líneas de lotes de un repuesto, con reservas ACTIVA agrupadas. */
    List<StockLote> porRepuesto(Connection conexion, int repuestoId);
    /** Una línea de lote, lista vacía si no existe. */
    List<StockLote> porLote(Connection conexion, int loteId, int repuestoId);
}
