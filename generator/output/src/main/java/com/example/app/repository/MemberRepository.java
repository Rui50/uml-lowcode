package com.example.app.repository;

import com.example.app.models.Member;
import jakarta.persistence.EntityManager;
import java.util.List;

public class MemberRepository {

    private final EntityManager em;

    public MemberRepository(EntityManager em) {
        this.em = em;
    }

    public Member save(Member entity) {
        if (entity.getId() == null) {
            em.persist(entity);
            return entity;
        }
        return em.merge(entity);
    }

    public Member findById(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("from Member", Member.class).getResultList();
    }

    public void deleteById(Long id) {
        Member entity = em.find(Member.class, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

}
