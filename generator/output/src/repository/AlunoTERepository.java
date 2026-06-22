package repository;

import models.AlunoTE;
import java.util.*;

public class AlunoTERepository {

    private Map<Long, AlunoTE> data = new HashMap<>();
    private Long nextId = 1L;

    public AlunoTE save(AlunoTE entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public AlunoTE findById(Long id) {
        return data.get(id);
    }

    public List<AlunoTE> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
