package com.rupeek.maidbooking.infrastructure.persistence;

import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.port.MaidRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMaidRepository implements MaidRepository {

    private final Map<UUID, Maid> store = new ConcurrentHashMap<>();

    @Override
    public Maid save(Maid maid) {
        store.put(maid.getId(), maid);
        return maid;
    }

    @Override
    public Optional<Maid> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Maid> findAll() {
        return new ArrayList<>(store.values());
    }
}
