package repository;

import models.Author;
import java.util.*;

public class AuthorRepository {

    private Map<Long, Author> data = new HashMap<>();
    private Long nextId = 1L;

    public Author save(Author entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Author findById(Long id) {
        return data.get(id);
    }

    public List<Author> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
