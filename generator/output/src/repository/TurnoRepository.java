package repository;

import models.Turno;
import java.util.*;

public class TurnoRepository {

    private Map<Long, Turno> data = new HashMap<>();
    private Long nextId = 1L;

    public Turno save(Turno entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Turno findById(Long id) {
        return data.get(id);
    }

    public List<Turno> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
