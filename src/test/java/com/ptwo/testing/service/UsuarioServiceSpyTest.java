package com.ptwo.testing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ptwo.testing.model.Usuario;
import com.ptwo.testing.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
 class UsuarioServiceSpyTest {
    // Creamos un mock normal para el repositorio
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;
    
    // Pero un spy para el servicio de notificaciones
    @Spy
    private NotificacionServiceImpl notificacionService = new NotificacionServiceImpl();
    
    // E inyectamos ambos en nuestro servicio
    @InjectMocks
    private UsuarioService usuarioService;
    
    @Test
    void testConSpyComoInyeccion() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Cristina Lago", "cristina@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        
        // Modificamos comportamiento del spy para un método específico
        doThrow(new RuntimeException("Error simulado"))
            .when(notificacionService).enviarNotificacionDesactivacion(any());
        
        // Act: crearUsuario debería funcionar normalmente
        Usuario resultado = usuarioService.crearUsuario(usuario);
        assertNotNull(resultado);
        
        // Pero desactivarUsuario debería fallar
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        assertThrows(RuntimeException.class, () -> {
            usuarioService.desactivarUsuario(1L);
        });
        
        // Verify: el spy permite verificaciones como un mock normal
        verify(notificacionService).enviarNotificacionRegistro(usuario);
    }

    @Test
    void multipleReturnsSecuenciales() {
        // Configuramos respuestas secuenciales
        when(usuarioRepository.findAll())
            .thenReturn(Arrays.asList(new Usuario(1L, "Usuario1", "email1@ejemplo.com")))  // Primera llamada
            .thenReturn(Arrays.asList(
                new Usuario(2L, "Usuario2", "email2@ejemplo.com"),
                new Usuario(3L, "Usuario3", "email3@ejemplo.com")))  // Segunda llamada
            .thenReturn(Collections.emptyList());  // Tercera llamada y siguientes
        
        // Primera llamada
        List<Usuario> resultado1 = usuarioService.obtenerTodosLosUsuarios();
        assertEquals(1, resultado1.size());
        assertEquals("Usuario1", resultado1.get(0).getNombre());
        
        // Segunda llamada
        List<Usuario> resultado2 = usuarioService.obtenerTodosLosUsuarios();
        assertEquals(2, resultado2.size());
        assertEquals("Usuario2", resultado2.get(0).getNombre());
        assertEquals("Usuario3", resultado2.get(1).getNombre());
        
        // Tercera llamada
        List<Usuario> resultado3 = usuarioService.obtenerTodosLosUsuarios();
        assertTrue(resultado3.isEmpty());
        
        // Cuarta llamada (sigue devolviendo el último valor configurado)
        List<Usuario> resultado4 = usuarioService.obtenerTodosLosUsuarios();
        assertTrue(resultado4.isEmpty());
    }

    @Test
    void returnsYThrows() {
        // Configuramos una secuencia que eventualmente lanza una excepción
        when(usuarioRepository.findById(eq(1L)))
            .thenReturn(Optional.of(new Usuario(1L, "Usuario Inicial", "inicial@ejemplo.com")))
            .thenReturn(Optional.of(new Usuario(1L, "Usuario Actualizado", "actualizado@ejemplo.com")))
            .thenThrow(new RuntimeException("Base de datos no disponible"));
        
        // Primera llamada - devuelve valor
        Optional<Usuario> resultado1 = usuarioService.obtenerUsuario(1L);
        assertTrue(resultado1.isPresent());
        assertEquals("Usuario Inicial", resultado1.get().getNombre());
        
        // Segunda llamada - devuelve otro valor
        Optional<Usuario> resultado2 = usuarioService.obtenerUsuario(1L);
        assertTrue(resultado2.isPresent());
        assertEquals("Usuario Actualizado", resultado2.get().getNombre());
        
        // Tercera llamada - lanza excepción
        Exception exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(1L);
        });
        assertEquals("Base de datos no disponible", exception.getMessage());
    }

    @Test
    void respuestasDinamicas() {
        // Usamos un contador atómico para llevar la cuenta de las llamadas
        AtomicInteger contador = new AtomicInteger(0);
        
        // Configuramos un comportamiento dinámico basado en el contador
        when(usuarioRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            int numLlamada = contador.getAndIncrement();
            
            if (numLlamada == 0) {
                return Optional.of(new Usuario(id, "Primera llamada", "primera@ejemplo.com"));
            } else if (numLlamada == 1) {
                return Optional.of(new Usuario(id, "Segunda llamada", "segunda@ejemplo.com"));
            } else if (numLlamada < 5) {
                return Optional.of(new Usuario(id, "Llamada intermedia", "intermedia@ejemplo.com"));
            } else {
                throw new RuntimeException("Demasiadas consultas");
            }
        });
        
        // Probamos el comportamiento dinámico
        for (int i = 0; i < 5; i++) {
            Optional<Usuario> resultado = usuarioService.obtenerUsuario(5L);
            assertTrue(resultado.isPresent());
            if (i == 0) {
                assertEquals("Primera llamada", resultado.get().getNombre());
            } else if (i == 1) {
                assertEquals("Segunda llamada", resultado.get().getNombre());
            } else {
                assertEquals("Llamada intermedia", resultado.get().getNombre());
            }
        }
        
        // La sexta llamada debería lanzar excepción
        assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(5L);
        });
    }

    @Test
    void returnsPorArgumentos() {
        // Configuramos diferentes respuestas según el ID
        when(usuarioRepository.findById(eq(1L)))
            .thenReturn(Optional.of(new Usuario(1L, "Admin", "admin@ejemplo.com")));
        
        when(usuarioRepository.findById(eq(2L)))
            .thenReturn(Optional.of(new Usuario(2L, "Usuario", "usuario@ejemplo.com")));
        
        when(usuarioRepository.findById(eq(3L)))
            .thenReturn(Optional.empty());
        
        when(usuarioRepository.findById(argThat(id -> id > 1000)))
            .thenThrow(new RuntimeException("ID fuera de rango"));
        
        // Probamos diferentes IDs
        assertTrue(usuarioService.obtenerUsuario(1L).isPresent());
        assertEquals("Admin", usuarioService.obtenerUsuario(1L).get().getNombre());
        
        assertTrue(usuarioService.obtenerUsuario(2L).isPresent());
        assertEquals("Usuario", usuarioService.obtenerUsuario(2L).get().getNombre());
        
        assertFalse(usuarioService.obtenerUsuario(3L).isPresent());
        
        assertThrows(RuntimeException.class, () -> {
            usuarioService.obtenerUsuario(1001L);
        });
    }

    @Test
    void verificacionBasicaMetodoVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Pedro García", "pedro@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Act: llamamos a métodos que ejecutan operaciones void
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: verificamos que los métodos void fueron llamados
        verify(notificacionService).enviarNotificacionRegistro(usuario);
        verify(notificacionService).enviarNotificacionDesactivacion(any());
    }

    @Test
    void verificarNumeroLlamadasVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Sandra Ruiz", "sandra@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.of(usuario));
        
        // Act: llamamos los métodos múltiples veces
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        usuarioService.desactivarUsuario(2L);
        usuarioService.desactivarUsuario(3L);
        
        // Verify: comprobamos el número exacto de llamadas
        verify(notificacionService, times(1)).enviarNotificacionRegistro(any());
        verify(notificacionService, times(3)).enviarNotificacionDesactivacion(any());
        
        // Alternativas para verificar número de llamadas
        verify(notificacionService, atLeastOnce()).enviarNotificacionRegistro(any());
        verify(notificacionService, atLeast(2)).enviarNotificacionDesactivacion(any());
        verify(notificacionService, atMost(5)).enviarNotificacionDesactivacion(any());
        verify(usuarioRepository, never()).delete(anyLong());
    }

    @Test
    void capturarArgumentosMetodoVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Roberto Núñez", "roberto@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Preparamos captores de argumentos
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<String> operacionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detallesCaptor = ArgumentCaptor.forClass(String.class);
        
        // Act
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: capturamos y analizamos los argumentos
        verify(notificacionService, times(1)).enviarNotificacionRegistro(usuarioCaptor.capture());
        verify(auditoriaService, atLeastOnce()).registrarOperacion(
            operacionCaptor.capture(), detallesCaptor.capture());
        
        // Obtenemos todos los valores capturados
        List<Usuario> usuariosCapturados = usuarioCaptor.getAllValues();
        List<String> operacionesCapturadas = operacionCaptor.getAllValues();
        
        // Analizamos los valores capturados
        assertEquals(1, usuariosCapturados.size());
        assertEquals("Roberto Núñez", usuariosCapturados.get(0).getNombre());
        assertTrue(operacionesCapturadas.contains("CREAR_USUARIO"));
    }

    @Test
    void verificarNoLlamadaMetodoVoid() {
        // Arrange: configuramos el repositorio para devolver Optional.empty()
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act: intentamos desactivar un usuario que no existe
        usuarioService.desactivarUsuario(99L);
        
        // Verify: no debería enviarse ninguna notificación ni guardarse nada
        verify(notificacionService, never()).enviarNotificacionDesactivacion(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verificarOrdenLlamadasVoid() {
        // Arrange
        Usuario usuario = new Usuario(1L, "Carolina Silva", "carolina@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        
        // Act: realizamos operaciones en cierto orden
        usuarioService.crearUsuario(usuario);
        usuarioService.desactivarUsuario(1L);
        
        // Verify: verificamos que las operaciones ocurrieron en el orden esperado
        InOrder orden = inOrder(notificacionService, auditoriaService);
        
        // Primera debería enviarse la notificación de registro
        orden.verify(notificacionService).enviarNotificacionRegistro(any());
        
        // Luego la notificación de desactivación
        orden.verify(notificacionService).enviarNotificacionDesactivacion(any());
        
        // Y finalmente debería registrarse en auditoría (asumiendo que esto ocurre al final)
        orden.verify(auditoriaService, atLeastOnce()).registrarOperacion(anyString(), anyString());
    }

    @Test
    void capturaBasicaDeArgumentos() {
        // Creamos un captor para el tipo Usuario
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        
        // Simulamos una operación
        Usuario usuario = new Usuario(1L, "Marta López", "marta@ejemplo.com");
        when(usuarioRepository.save(any())).thenReturn(usuario);
        usuarioService.crearUsuario(usuario);
        
        // Verificamos la llamada y capturamos el argumento
        verify(usuarioRepository).save(usuarioCaptor.capture());
        
        // Accedemos al valor capturado
        Usuario usuarioCapturado = usuarioCaptor.getValue();
        
        // Realizamos verificaciones sobre el valor capturado
        assertEquals("Marta López", usuarioCapturado.getNombre());
        assertEquals("marta@ejemplo.com", usuarioCapturado.getEmail());
        assertTrue(usuarioCapturado.isActivo());
    }

    @Test
    void capturaMultiplesArgumentos() {
        // Captores para diferentes argumentos
        ArgumentCaptor<String> tipoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> detallesCaptor = ArgumentCaptor.forClass(String.class);
        
        // Simulamos varias operaciones
        Usuario usuario1 = new Usuario(1L, "Ana Gil", "ana@ejemplo.com");
        Usuario usuario2 = new Usuario(2L, "Mario Ros", "mario@ejemplo.com");
        
        when(usuarioRepository.save(any())).thenReturn(usuario1).thenReturn(usuario2);
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.of(usuario1));
        
        usuarioService.crearUsuario(usuario1);
        usuarioService.crearUsuario(usuario2);
        usuarioService.desactivarUsuario(1L);
        
        // Verificamos y capturamos todas las llamadas
        verify(auditoriaService, times(3)).registrarOperacion(
            tipoCaptor.capture(), detallesCaptor.capture());
        
        // Obtenemos todas las capturas
        List<String> tipos = tipoCaptor.getAllValues();
        List<String> detalles = detallesCaptor.getAllValues();
        
        // Verificamos los valores capturados
        assertEquals(3, tipos.size());
        assertEquals(3, detalles.size());
        
        // Verificamos el contenido específico
        assertTrue(tipos.contains("CREAR_USUARIO"));
        assertTrue(tipos.contains("DESACTIVAR_USUARIO"));
        
        boolean encontradoAna = false;
        boolean encontradoMario = false;
        
        for(String detalle : detalles) {
            if(detalle.contains("Ana Gil")) encontradoAna = true;
            if(detalle.contains("Mario Ros")) encontradoMario = true;
        }
        
        assertTrue(encontradoAna, "Debería encontrarse Ana en los detalles");
        assertTrue(encontradoMario, "Debería encontrarse Mario en los detalles");
    }

    @Test
    void verificacionesAvanzadasConCaptura() {
        // Modificamos temporalmente nuestro UsuarioService para este test
        // Imaginemos que ahora tiene un método que guarda múltiples usuarios a la vez
        
        // Simulamos una implementación adicional
        class UsuarioServiceExtendido extends UsuarioService {
            public UsuarioServiceExtendido(UsuarioRepository repo, 
                                        NotificacionService notif,
                                        AuditoriaService audit) {
                super(repo, notif, audit);
            }
            
            public List<Usuario> crearUsuariosEnLote(List<Usuario> usuarios) {
                List<Usuario> resultado = new ArrayList<>();
                for(Usuario u : usuarios) {
                    if(u.getEmail() != null && u.getEmail().contains("@")) {
                        Usuario guardado = usuarioRepository.save(u);
                        notificacionService.enviarNotificacionRegistro(guardado);
                        resultado.add(guardado);
                    }
                }
                auditoriaService.registrarOperacion("CREAR_LOTE", 
                    "Creados " + resultado.size() + " usuarios en lote");
                return resultado;
            }
        }
        
        // Creamos una instancia del servicio extendido
        UsuarioServiceExtendido servicioExtendido = new UsuarioServiceExtendido(
            usuarioRepository, notificacionService, auditoriaService);
        
        // Preparamos datos y captores
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        
        List<Usuario> loteUsuarios = Arrays.asList(
            new Usuario(1L, "User1", "user1@ejemplo.com"),
            new Usuario(2L, "User2", "user2@ejemplo.com"),
            new Usuario(3L, "User3", "emailinvalido"),  // Email inválido
            new Usuario(4L, "User4", "user4@ejemplo.com")
        );
        
        // Configuramos el mock para devolver el mismo usuario que recibe
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        // Act
        List<Usuario> resultado = servicioExtendido.crearUsuariosEnLote(loteUsuarios);
        
        // Verify: capturamos todos los usuarios guardados
        verify(usuarioRepository, times(3)).save(usuarioCaptor.capture());
        
        // Obtenemos los valores capturados
        List<Usuario> usuariosGuardados = usuarioCaptor.getAllValues();
        
        // Verificaciones avanzadas
        assertEquals(3, usuariosGuardados.size());
        assertEquals(3, resultado.size());
        
        // Verificamos que no se guardó el usuario con email inválido
        boolean encontradoInvalido = false;
        for(Usuario u : usuariosGuardados) {
            if("User3".equals(u.getNombre())) {
                encontradoInvalido = true;
                break;
            }
        }
        
        assertFalse(encontradoInvalido, "No debería guardarse el usuario con email inválido");
        
        // Verificamos que se enviaron las notificaciones correctas
        verify(notificacionService, times(3)).enviarNotificacionRegistro(any());
        
        // Y verificamos la auditoría
        verify(auditoriaService).registrarOperacion(eq("CREAR_LOTE"), contains("3 usuarios"));
    }

}
