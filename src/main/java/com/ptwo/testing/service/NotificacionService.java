package com.ptwo.testing.service;

import com.ptwo.testing.model.Usuario;

public interface NotificacionService {
    void enviarNotificacionRegistro(Usuario usuario);
    void enviarNotificacionDesactivacion(Usuario usuario);
}