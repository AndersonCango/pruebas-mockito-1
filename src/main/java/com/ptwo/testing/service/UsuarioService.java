package com.ptwo.testing.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.ptwo.testing.model.Usuario;
import com.ptwo.testing.repository.UsuarioRepository;

public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          NotificacionService notificacionService,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    public Usuario crearUsuario(Usuario usuario) {
        if (usuario.getEmail() == null || !usuario.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email inválido");
        }

        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        notificacionService.enviarNotificacionRegistro(usuario);
        auditoriaService.registrarOperacion("CREAR_USUARIO",
                "Usuario creado: " + usuario.getNombre() + " (" + usuario.getEmail() + ")");
        return usuarioGuardado;
    }

    public Optional<Usuario> obtenerUsuario(Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> obtenerTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    public void desactivarUsuario(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
            notificacionService.enviarNotificacionDesactivacion(usuario);
            auditoriaService.registrarOperacion("DESACTIVAR_USUARIO",
                    "Usuario desactivado: " + usuario.getNombre());
        }
    }

     public CompletableFuture<Usuario> crearUsuarioAsync(Usuario usuario) {
        return CompletableFuture.supplyAsync(() -> {
            return crearUsuario(usuario);
        });
    }
}