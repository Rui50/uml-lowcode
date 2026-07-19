package com.example.app.repository;

import com.example.app.models.LibraryCard;
import jakarta.persistence.EntityManager;
import java.util.List;

public class LibraryCardRepository {

    private final EntityManager em;

    public LibraryCardRepository(EntityManager em) {
        this.em = em;
    }

    public LibraryCard save(LibraryCard entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public LibraryCard findById(Long id) {
        return em.find(LibraryCard.class, id);
    }

    public List<LibraryCard> findAll() {
        return em.createQuery("from LibraryCard", LibraryCard.class).getResultList();
    }

    public void deleteById(Long id) {
        LibraryCard entity = em.find(LibraryCard.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
