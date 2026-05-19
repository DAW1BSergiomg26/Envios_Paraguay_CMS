package com.grupb2.casarural.service;

import com.grupb2.casarural.model.Cliente;
import com.grupb2.casarural.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    public Optional<Cliente> autenticar(String email, String password) {
        return repo.findByEmail(email)
                .filter(c -> c.getPassword().equals(password));
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return repo.findById(id);
    }

    public Cliente guardar(Cliente cliente) {
        return repo.save(cliente);
    }

    public java.util.List<Cliente> listarTodos() {
        return repo.findAll();
    }
}
