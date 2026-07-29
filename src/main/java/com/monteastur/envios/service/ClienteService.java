package com.monteastur.envios.service;

import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.repository.ClienteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repo;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Cliente> autenticar(String email, String password) {
        Optional<Cliente> opt = repo.findByEmail(email);
        if (opt.isEmpty()) return Optional.empty();

        Cliente c = opt.get();
        String stored = c.getPassword();
        if (stored == null) return Optional.empty();

        if (esBcrypt(stored)) {
            if (passwordEncoder.matches(password, stored)) {
                return opt;
            }
        } else {
            if (stored.equals(password)) {
                c.setPassword(passwordEncoder.encode(password));
                repo.save(c);
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Cacheable("envios.clientes")
    public Optional<Cliente> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @CacheEvict(value = "envios.clientes", allEntries = true)
    public Cliente guardar(Cliente cliente) {
        if (!esBcrypt(cliente.getPassword())) {
            cliente.setPassword(passwordEncoder.encode(cliente.getPassword()));
        }
        return repo.save(cliente);
    }

    @Cacheable("envios.clientes")
    public java.util.List<Cliente> listarTodos() {
        return repo.findAll();
    }

    private boolean esBcrypt(String password) {
        return password != null && (
            password.startsWith("$2a$") ||
            password.startsWith("$2b$") ||
            password.startsWith("$2y$")
        );
    }
}
