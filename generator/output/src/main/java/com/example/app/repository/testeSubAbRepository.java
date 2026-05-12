package com.example.app.repository;

import com.example.app.models.testeSubAb;
import jakarta.persistence.EntityManager;
import java.util.List;

public class testeSubAbRepository {

    private final EntityManager em;

    public testeSubAbRepository(EntityManager em) {
        this.em = em;
    }

    public testeSubAb save(testeSubAb entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public testeSubAb findById(Long id) {
        return em.find(testeSubAb.class, id);
    }

    public List<testeSubAb> findAll() {
        return em.createQuery("from testeSubAb", testeSubAb.class).getResultList();
    }

    public void deleteById(Long id) {
        testeSubAb entity = em.find(testeSubAb.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
