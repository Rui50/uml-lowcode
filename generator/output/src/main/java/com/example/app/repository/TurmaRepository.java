package com.example.app.repository;

import com.example.app.models.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Turma entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface TurmaRepository extends JpaRepository<Turma, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Turma> findByNome(String nome);
    List<Turma> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

}
