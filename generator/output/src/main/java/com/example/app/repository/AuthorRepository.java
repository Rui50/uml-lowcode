package com.example.app.repository;

import com.example.app.models.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Author entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Author> findByName(String name);
    List<Author> findByNameContaining(String name);
    boolean existsByName(String name);

}
