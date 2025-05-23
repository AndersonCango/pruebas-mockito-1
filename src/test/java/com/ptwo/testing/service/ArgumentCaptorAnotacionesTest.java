package com.ptwo.testing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ptwo.testing.model.Usuario;
import com.ptwo.testing.repository.UsuarioRepository;
import com.ptwo.testing.repository.UsuarioRepositoryImpl;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

 @ExtendWith(MockitoExtension.class)
 class ArgumentCaptorAnotacionesTest {
    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private NotificacionService notificacionService;
    
    @Mock
    private AuditoriaService auditoriaService;
    
    @InjectMocks
    private UsuarioService usuarioService;
    
    @Captor
    private ArgumentCaptor<Usuario> usuarioCaptor;
    
    @Captor
    private ArgumentCaptor<String> stringCaptor;
    
    @Test
    void testCapturadorConAnotaciones() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Jaime Vega", "jaime@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        // Act
        usuarioService.crearUsuario(usuario);
        
        // Verify con los captores inyectados (por @Captor)
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(auditoriaService).registrarOperacion(eq("CREAR_USUARIO"), stringCaptor.capture());
        
        // Accedemos a los valores capturados
        Usuario usuarioCaptado = usuarioCaptor.getValue();
        String detallesCaptados = stringCaptor.getValue();
        
        // Verificamos
        assertEquals("Jaime Vega", usuarioCaptado.getNombre());
        assertTrue(detallesCaptados.contains("Jaime Vega"));
    }

    @Test
    void testMockearMetodoFinal() {
        // Crear el mock de la clase que contiene métodos finales
        ClaseConMetodosFinal mock = mock(ClaseConMetodosFinal.class);
        
        // Configurar comportamiento del método final
        when(mock.metodoFinal()).thenReturn("Resultado mockeado");
        when(mock.metodoFinalConParametro(anyString())).thenReturn("Parámetro mockeado");
        
        // Verificar comportamiento
        assertEquals("Resultado mockeado", mock.metodoFinal());
        assertEquals("Parámetro mockeado", mock.metodoFinalConParametro("test"));
        
        // Verificar llamadas
        verify(mock).metodoFinal();
        verify(mock).metodoFinalConParametro("test");
    }

    @Test
    void testMockRepositorioConMetodosFinales() {
        // Crear mock de la implementación concreta
        UsuarioRepositoryImpl repositoryMock = mock(UsuarioRepositoryImpl.class);
        
        // Configurar comportamiento
        Usuario usuario = new Usuario(1L, "Sofia Castro", "sofia@ejemplo.com");
        when(repositoryMock.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Utilizar el mock
        Optional<Usuario> resultado = repositoryMock.findById(1L);
        
        // Verificar
        assertTrue(resultado.isPresent());
        assertEquals("Sofia Castro", resultado.get().getNombre());
    }

    @Test
    void testRutaExito() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Usuario", 
    "email@valido.com")));
        assertTrue(usuarioService.obtenerUsuario(1L).isPresent());
    }

    @Test
    void testRutaFallo() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertFalse(usuarioService.obtenerUsuario(99L).isPresent());
    }

    @Test
    void testLogicaInternaConCaptor() {
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        
        // Act: llamamos a un método que debe modificar el usuario antes de guardarlo
        usuarioService.crearUsuario(new Usuario(1L, "Daniel Sam", "dam@ejemplo.com"));
        
        // Capture: capturamos el usuario que se pasa al repositorio
        verify(usuarioRepository).save(usuarioCaptor.capture());
        
        // Assert: verificamos que la lógica interna funcionó correctamente
        Usuario usuarioCapturado = usuarioCaptor.getValue();
        assertTrue(usuarioCapturado.isActivo()); // Verificamos que el flag por defecto se estableció
        assertNotNull(usuarioCapturado.getId()); // Verificamos que se generó un ID
    }
}
