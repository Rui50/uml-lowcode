package com.example.app.repository;

import com.example.app.models.Loan;
import jakarta.persistence.EntityManager;
import java.util.List;

public class LoanRepository {

    private final EntityManager em;

    public LoanRepository(EntityManager em) {
        this.em = em;
    }

    public Loan save(Loan entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Loan findById(Long id) {
        return em.find(Loan.class, id);
    }

    public List<Loan> findAll() {
        return em.createQuery("from Loan", Loan.class).getResultList();
    }

    public void deleteById(Long id) {
        Loan entity = em.find(Loan.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
