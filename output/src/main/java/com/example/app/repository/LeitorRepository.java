package com.example.app.repository;

import com.example.app.models.Leitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Leitor entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface LeitorRepository extends JpaRepository<Leitor, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Leitor> findByNome(String nome);
    List<Leitor> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

    List<Leitor> findByEmail(String email);
    List<Leitor> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
