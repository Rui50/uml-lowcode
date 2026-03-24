package com.example.app.repository;

import com.example.app.models.Exemplar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Exemplar entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface ExemplarRepository extends JpaRepository<Exemplar, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Exemplar> findByCodigo(String codigo);
    List<Exemplar> findByCodigoContaining(String codigo);
    boolean existsByCodigo(String codigo);

}
