package repository;

import models.Library;
import java.util.*;

public class LibraryRepository {

    private Map<Long, Library> data = new HashMap<>();
    private Long nextId = 1L;

    public Library save(Library entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Library findById(Long id) {
        return data.get(id);
    }

    public List<Library> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
