package com.example.app.repository;

import com.example.app.models.AlunoTE;
import jakarta.persistence.EntityManager;
import java.util.List;

public class AlunoTERepository {

    private final EntityManager em;

    public AlunoTERepository(EntityManager em) {
        this.em = em;
    }

    public AlunoTE save(AlunoTE entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public AlunoTE findById(Long id) {
        return em.find(AlunoTE.class, id);
    }

    public List<AlunoTE> findAll() {
        return em.createQuery("from AlunoTE", AlunoTE.class).getResultList();
    }

    public void deleteById(Long id) {
        AlunoTE entity = em.find(AlunoTE.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
