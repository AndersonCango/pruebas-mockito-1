package com.ptwo.testing.service;

import com.ptwo.testing.model.Usuario;

public class NotificacionServiceImpl implements NotificacionService {
    @Override
    public void enviarNotificacionRegistro(Usuario usuario) {
        // Implementación real: en un test, probablemente no haga nada
        System.out.println("Enviando notificación de registro a " + usuario.getEmail());
    }
    
    @Override
    public void enviarNotificacionDesactivacion(Usuario usuario) {
        System.out.println("Enviando notificación de desactivación a " + usuario.getEmail());
    }
}
