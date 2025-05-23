package com.ptwo.testing.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.ptwo.testing.model.Usuario;

public class UsuarioRepositoryImpl implements UsuarioRepository {
    @Override
    public final Optional<Usuario> findById(Long id) {
        // Implementación real que conectaría con la base de datos
        return Optional.empty();
    }
    
    @Override
    public final List<Usuario> findAll() {
        // Implementación real
        return Collections.emptyList();
    }
    
    @Override
    public Usuario save(Usuario usuario) {
        // Implementación real que conectaría con la base de datos
        return usuario;
    }

    @Override
    public void delete(Long id) {
        // Implementación real que conectaría con la base de datos
    }

    @Override
    public boolean existsById(Long id) {
        // Implementación real que conectaría con la base de datos
        return false;
    }
}
