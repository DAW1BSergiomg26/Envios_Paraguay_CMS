package com.monteastur.envios.service;

import com.monteastur.envios.model.MensajeContacto;
import com.monteastur.envios.repository.MensajeContactoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MensajeContactoService {

    private final MensajeContactoRepository repo;

    public MensajeContactoService(MensajeContactoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<MensajeContacto> listar(Boolean leido) {
        List<MensajeContacto> mensajes = repo.findAllByOrderByFechaEnvioDesc();
        if (leido == null) {
            return mensajes;
        }
        return mensajes.stream().filter(m -> m.isLeido() == leido).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MensajeContacto> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional
    public Optional<MensajeContacto> marcarLeido(Long id, boolean leido) {
        return repo.findById(id).map(m -> {
            m.setLeido(leido);
            return repo.save(m);
        });
    }

    @Transactional
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
