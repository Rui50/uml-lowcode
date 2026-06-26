package com.example.app.repository;

import com.example.app.models.LinhaEncomenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LinhaEncomenda entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface LinhaEncomendaRepository extends JpaRepository<LinhaEncomenda, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // custom query methods TODO

}
