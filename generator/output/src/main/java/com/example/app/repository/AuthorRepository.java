package com.example.app.repository;

import com.example.app.models.Author;
import jakarta.persistence.EntityManager;
import java.util.List;

public class AuthorRepository {

    private final EntityManager em;

    public AuthorRepository(EntityManager em) {
        this.em = em;
    }

    public Author save(Author entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Author findById(Long id) {
        return em.find(Author.class, id);
    }

    public List<Author> findAll() {
        return em.createQuery("from Author", Author.class).getResultList();
    }

    public void deleteById(Long id) {
        Author entity = em.find(Author.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
