package repository;

import models.LibraryCard;
import java.util.*;

public class LibraryCardRepository {

    private Map<Long, LibraryCard> data = new HashMap<>();
    private Long nextId = 1L;

    public LibraryCard save(LibraryCard entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public LibraryCard findById(Long id) {
        return data.get(id);
    }

    public List<LibraryCard> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
