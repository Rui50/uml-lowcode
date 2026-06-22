package repository;

import models.Aluno;
import java.util.*;

public class AlunoRepository {

    private Map<Long, Aluno> data = new HashMap<>();
    private Long nextId = 1L;

    public Aluno save(Aluno entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Aluno findById(Long id) {
        return data.get(id);
    }

    public List<Aluno> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
