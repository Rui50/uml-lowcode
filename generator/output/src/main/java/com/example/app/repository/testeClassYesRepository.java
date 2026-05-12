package com.example.app.repository;

import com.example.app.models.testeClassYes;
import jakarta.persistence.EntityManager;
import java.util.List;

public class testeClassYesRepository {

    private final EntityManager em;

    public testeClassYesRepository(EntityManager em) {
        this.em = em;
    }

    public testeClassYes save(testeClassYes entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public testeClassYes findById(Long id) {
        return em.find(testeClassYes.class, id);
    }

    public List<testeClassYes> findAll() {
        return em.createQuery("from testeClassYes", testeClassYes.class).getResultList();
    }

    public void deleteById(Long id) {
        testeClassYes entity = em.find(testeClassYes.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
