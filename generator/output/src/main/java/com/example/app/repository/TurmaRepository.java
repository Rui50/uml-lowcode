package com.example.app.repository;

import com.example.app.models.Turma;
import jakarta.persistence.EntityManager;
import java.util.List;

public class TurmaRepository {

    private final EntityManager em;

    public TurmaRepository(EntityManager em) {
        this.em = em;
    }

    public Turma save(Turma entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Turma findById(Long id) {
        return em.find(Turma.class, id);
    }

    public List<Turma> findAll() {
        return em.createQuery("from Turma", Turma.class).getResultList();
    }

    public void deleteById(Long id) {
        Turma entity = em.find(Turma.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
