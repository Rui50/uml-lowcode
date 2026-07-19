package com.example.app.repository;

import com.example.app.models.Book;
import jakarta.persistence.EntityManager;
import java.util.List;

public class BookRepository {

    private final EntityManager em;

    public BookRepository(EntityManager em) {
        this.em = em;
    }

    public Book save(Book entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Book findById(Long id) {
        return em.find(Book.class, id);
    }

    public List<Book> findAll() {
        return em.createQuery("from Book", Book.class).getResultList();
    }

    public void deleteById(Long id) {
        Book entity = em.find(Book.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
