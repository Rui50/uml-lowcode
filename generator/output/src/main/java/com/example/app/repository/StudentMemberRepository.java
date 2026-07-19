package com.example.app.repository;

import com.example.app.models.StudentMember;
import jakarta.persistence.EntityManager;
import java.util.List;

public class StudentMemberRepository {

    private final EntityManager em;

    public StudentMemberRepository(EntityManager em) {
        this.em = em;
    }

    public StudentMember save(StudentMember entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public StudentMember findById(Long id) {
        return em.find(StudentMember.class, id);
    }

    public List<StudentMember> findAll() {
        return em.createQuery("from StudentMember", StudentMember.class).getResultList();
    }

    public void deleteById(Long id) {
        StudentMember entity = em.find(StudentMember.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
