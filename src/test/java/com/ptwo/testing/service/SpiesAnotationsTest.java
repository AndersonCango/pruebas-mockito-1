package com.ptwo.testing.service;
 import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

 @ExtendWith(MockitoExtension.class)
 class SpiesAnotacionesTest {
    // Spy de ArrayList vacía (Mockito creará una instancia por nosotros)
    @Spy
    private List<String> listaSpy = new ArrayList<>();
    
    // Alternativa: Mockito creará la instancia usando el constructor sin argumentos
    @Spy
    private ArrayList<Integer> numerosSpy;
    
    @Test
    void testSpyConAnotaciones() {
        // Los métodos no mockeados llaman a la implementación real
        listaSpy.add("uno");
        assertEquals(1, listaSpy.size());
        assertEquals("uno", listaSpy.get(0));
        
        // Podemos modificar el comportamiento de algunos métodos
        doReturn(100).when(listaSpy).size();
        assertEquals(100, listaSpy.size());
        
        // Y seguir utilizando el comportamiento real para otros
        listaSpy.add("dos");
        assertEquals("dos", listaSpy.get(1));
    }

     @Test
    void testSpyMantieneEstado() {
        listaSpy.add("uno"); // Modifica el estado real
        listaSpy.add("dos"); // Modifica el estado real
        
        assertEquals(2, listaSpy.size()); // Llama al método real que refleja el estado
        
        // Ahora mockeamos el método size
        doReturn(100).when(listaSpy).size();
        assertEquals(100, listaSpy.size()); // Devuelve el valor mockeado
        
        // Pero el estado interno sigue siendo real
        assertEquals("uno", listaSpy.get(0));
        assertEquals("dos", listaSpy.get(1));
        
        // Y seguirá cambiando con nuevas llamadas
        listaSpy.add("tres");
        assertEquals("tres", listaSpy.get(2));
    }

    @Test
    void testResetSpy() {
        // Configuramos el spy
        doReturn(100).when(listaSpy).size();
        assertEquals(100, listaSpy.size());
        
        // Reseteamos el spy
        reset(listaSpy);
        
        // Ahora vuelve a comportarse como el objeto real
        listaSpy.add("uno");
        assertEquals(1, listaSpy.size());
    }
}