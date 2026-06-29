package com.example.app.repository;

import com.example.app.models.StudentMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for StudentMember entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface StudentMemberRepository extends JpaRepository<StudentMember, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<StudentMember> findByCourse(String course);
    List<StudentMember> findByCourseContaining(String course);
    boolean existsByCourse(String course);

    List<StudentMember> findByName(String name);
    List<StudentMember> findByNameContaining(String name);
    boolean existsByName(String name);

    List<StudentMember> findByEmail(String email);
    List<StudentMember> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
