package com.example.app.repository;

import com.example.app.models.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Turno entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // custom query methods TODO

}
