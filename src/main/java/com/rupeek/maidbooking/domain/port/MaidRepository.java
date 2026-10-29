package com.rupeek.maidbooking.domain.port;

import com.rupeek.maidbooking.domain.model.Maid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for Maid entities.
 * Decouples business logic from in-memory collections vs PostgreSQL JPA/JDBC.
 */
public interface MaidRepository {
    Maid save(Maid maid);

    Optional<Maid> findById(UUID id);

    List<Maid> findAll();
}
