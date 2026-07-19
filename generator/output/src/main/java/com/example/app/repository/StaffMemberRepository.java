package com.example.app.repository;

import com.example.app.models.StaffMember;
import jakarta.persistence.EntityManager;
import java.util.List;

public class StaffMemberRepository {

    private final EntityManager em;

    public StaffMemberRepository(EntityManager em) {
        this.em = em;
    }

    public StaffMember save(StaffMember entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public StaffMember findById(Long id) {
        return em.find(StaffMember.class, id);
    }

    public List<StaffMember> findAll() {
        return em.createQuery("from StaffMember", StaffMember.class).getResultList();
    }

    public void deleteById(Long id) {
        StaffMember entity = em.find(StaffMember.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
