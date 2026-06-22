package repository;

import models.Turma;
import java.util.*;

public class TurmaRepository {

    private Map<Long, Turma> data = new HashMap<>();
    private Long nextId = 1L;

    public Turma save(Turma entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Turma findById(Long id) {
        return data.get(id);
    }

    public List<Turma> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
