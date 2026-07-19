package com.example.app.repository;

import com.example.app.models.Library;
import jakarta.persistence.EntityManager;
import java.util.List;

public class LibraryRepository {

    private final EntityManager em;

    public LibraryRepository(EntityManager em) {
        this.em = em;
    }

    public Library save(Library entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Library findById(Long id) {
        return em.find(Library.class, id);
    }

    public List<Library> findAll() {
        return em.createQuery("from Library", Library.class).getResultList();
    }

    public void deleteById(Long id) {
        Library entity = em.find(Library.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
