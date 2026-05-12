package com.example.app.repository;

import com.example.app.models.ImplementInterface;
import jakarta.persistence.EntityManager;
import java.util.List;

public class ImplementInterfaceRepository {

    private final EntityManager em;

    public ImplementInterfaceRepository(EntityManager em) {
        this.em = em;
    }

    public ImplementInterface save(ImplementInterface entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public ImplementInterface findById(Long id) {
        return em.find(ImplementInterface.class, id);
    }

    public List<ImplementInterface> findAll() {
        return em.createQuery("from ImplementInterface", ImplementInterface.class).getResultList();
    }

    public void deleteById(Long id) {
        ImplementInterface entity = em.find(ImplementInterface.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
