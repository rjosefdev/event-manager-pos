package com.rjosefdev.eventos_api.eventos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventoRepository extends MongoRepository<Evento, String> {
    List<Evento> findAllByOrderByIniciaEmAsc();
    List<Evento> findByOrganizadorIdOrderByIniciaEmAsc(String organizadorId);
    Optional<Evento> findByIdAndOrganizadorId(String id, String organizadorId);
}
