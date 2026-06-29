package com.example.app.repository;

import com.example.app.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Member entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Member> findByName(String name);
    List<Member> findByNameContaining(String name);
    boolean existsByName(String name);

    List<Member> findByEmail(String email);
    List<Member> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
