package com.example.app.repository;

import com.example.app.models.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for StaffMember entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<StaffMember> findByDepartment(String department);
    List<StaffMember> findByDepartmentContaining(String department);
    boolean existsByDepartment(String department);

    List<StaffMember> findByName(String name);
    List<StaffMember> findByNameContaining(String name);
    boolean existsByName(String name);

    List<StaffMember> findByEmail(String email);
    List<StaffMember> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
