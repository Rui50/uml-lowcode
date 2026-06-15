package com.example.app.repository;

import com.example.app.models.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Aluno entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Aluno> findByNome(String nome);
    List<Aluno> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

    List<Aluno> findByEmail(String email);
    List<Aluno> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
