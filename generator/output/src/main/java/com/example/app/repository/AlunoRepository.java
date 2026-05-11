package com.example.app.repository;

import com.example.app.models.Aluno;
import jakarta.persistence.EntityManager;
import java.util.List;

public class AlunoRepository {

    private final EntityManager em;

    public AlunoRepository(EntityManager em) {
        this.em = em;
    }

    public Aluno save(Aluno entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Aluno findById(Long id) {
        return em.find(Aluno.class, id);
    }

    public List<Aluno> findAll() {
        return em.createQuery("from Aluno", Aluno.class).getResultList();
    }

    public void deleteById(Long id) {
        Aluno entity = em.find(Aluno.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
