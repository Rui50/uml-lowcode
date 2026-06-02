package com.example.app.repository;

import com.example.app.models.testeClassYes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for testeClassYes entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface testeClassYesRepository extends JpaRepository<testeClassYes, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<testeClassYes> findByTestar(String testar);
    List<testeClassYes> findByTestarContaining(String testar);
    boolean existsByTestar(String testar);

}
