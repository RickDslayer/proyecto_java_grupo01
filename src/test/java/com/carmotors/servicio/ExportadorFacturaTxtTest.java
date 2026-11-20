package com.carmotors.servicio;

import com.carmotors.excepcion.ExportacionException;
import com.carmotors.modelo.DatosOrdenParaFactura;
import com.carmotors.modelo.DocumentoFactura;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ExportadorFacturaTxtTest {
    @TempDir Path carpeta;
    private final ExportadorFacturaTxt exportador = new ExportadorFacturaTxt();
    private DocumentoFactura factura() {
        return new DocumentoFactura(7, 23, LocalDateTime.of(2026, 11, 19, 10, 30),
                "María Pérez", "123", "3001234567", null, "ABC123", "Toyota", "Corolla",
                new DatosOrdenParaFactura(List.of(new DatosOrdenParaFactura.Repuesto(1, "Filtro", 2, new BigDecimal("10.00"))),
                        List.of(new DatosOrdenParaFactura.ManoObra(4, "Cambio de aceite", new BigDecimal("30.00")))),
                new BigDecimal("20.00"), new BigDecimal("30.00"), new BigDecimal("9.50"), new BigDecimal("59.50"));
    }

    @Test void exportaIdentificacionDesgloseYTotalesEnUtf8() throws Exception {
        Path archivo = exportador.exportar(factura(), carpeta.resolve("nueva"));
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.contains("FACTURA #7"));
        assertTrue(contenido.contains("Orden de servicio: #23"));
        assertTrue(contenido.contains("María Pérez"));
        assertTrue(contenido.contains("Toyota Corolla | Placa: ABC123"));
        assertTrue(contenido.contains("cantidad: 2 | precio unitario: 10.00 | importe: 20.00"));
        assertTrue(contenido.contains("Cambio de aceite | importe: 30.00"));
        assertTrue(contenido.contains("Subtotal: 50.00\nImpuestos: 9.50\nTOTAL: 59.50"));
    }

    @Test void noSobrescribeArchivosYNoDejaTemporalesAlFallar() throws Exception {
        Path archivo = exportador.exportar(factura(), carpeta);
        Files.writeString(archivo, "conservar");
        assertThrows(ExportacionException.class, () -> exportador.exportar(factura(), carpeta));
        assertEquals("conservar", Files.readString(archivo));
        try (var archivos = Files.list(carpeta)) { assertEquals(1, archivos.count()); }
    }

    @Test void informaDestinoInvalidoYPermiteReintentarEnOtraCarpeta() throws Exception {
        Path invalido = carpeta.resolve("archivo.txt");
        Files.writeString(invalido, "no es una carpeta");
        assertThrows(ExportacionException.class, () -> exportador.exportar(factura(), invalido));
        assertTrue(Files.exists(exportador.exportar(factura(), carpeta.resolve("valida"))));
    }
}
