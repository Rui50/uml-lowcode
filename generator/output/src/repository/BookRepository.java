package repository;

import models.Book;
import java.util.*;

public class BookRepository {

    private Map<Long, Book> data = new HashMap<>();
    private Long nextId = 1L;

    public Book save(Book entity) {
        if (entity.getId() == null) {
            entity.setId(nextId++);
        }
        data.put(entity.getId(), entity);
        return entity;
    }

    public Book findById(Long id) {
        return data.get(id);
    }

    public List<Book> findAll() {
        return new ArrayList<>(data.values());
    }

    public boolean deleteById(Long id) {
        return data.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return data.containsKey(id);
    }

}
