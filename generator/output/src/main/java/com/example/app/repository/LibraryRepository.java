package com.example.app.repository;

import com.example.app.models.Library;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Library entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface LibraryRepository extends JpaRepository<Library, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Library> findByName(String name);
    List<Library> findByNameContaining(String name);
    boolean existsByName(String name);

}
