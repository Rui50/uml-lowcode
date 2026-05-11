package com.example.app.repository;

import com.example.app.models.Turno;
import jakarta.persistence.EntityManager;
import java.util.List;

public class TurnoRepository {

    private final EntityManager em;

    public TurnoRepository(EntityManager em) {
        this.em = em;
    }

    public Turno save(Turno entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Turno findById(Long id) {
        return em.find(Turno.class, id);
    }

    public List<Turno> findAll() {
        return em.createQuery("from Turno", Turno.class).getResultList();
    }

    public void deleteById(Long id) {
        Turno entity = em.find(Turno.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
