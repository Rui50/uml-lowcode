package com.example.app.repository;

import com.example.app.models.ImplementInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for ImplementInterface entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface ImplementInterfaceRepository extends JpaRepository<ImplementInterface, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // custom query methods TODO

}
