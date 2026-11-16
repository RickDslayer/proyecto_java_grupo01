package com.carmotors.modelo;

/** Resumen de unidades para un repuesto, sin mezclar unidades ni precios. */
public record StockRepuesto(int repuestoId, long fisico, long reservadoActivo,
                            long disponible, long disponibleUtilizable) { }
