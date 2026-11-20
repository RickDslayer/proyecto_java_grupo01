package com.carmotors.vista;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsolaTest {
    @Test void soloUnaConfirmacionExplicitaAutorizaLaOperacion() {
        assertTrue(consola("s\n").confirmar("¿Guardar?"));
        assertFalse(consola("N\n").confirmar("¿Guardar?"));
        assertFalse(consola("\nS\n").confirmar("¿Guardar?"));
        assertFalse(consola("").confirmar("¿Guardar?"));
    }

    @Test void respuestasInvalidasNoConfirmanYPermitenCorregir() {
        assertFalse(consola("cualquier texto\nN\n").confirmar("¿Eliminar?"));
        assertTrue(consola("0\nS\n").confirmar("¿Guardar?"));
    }

    private Consola consola(String entrada) {
        return new Consola(new Scanner(entrada), new PrintStream(new ByteArrayOutputStream()));
    }
}
