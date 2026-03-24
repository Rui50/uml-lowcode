package com.example.app.repository;

import com.example.app.models.AlunoTE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for AlunoTE entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface AlunoTERepository extends JpaRepository<AlunoTE, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<AlunoTE> findByEmpresa(String empresa);
    List<AlunoTE> findByEmpresaContaining(String empresa);
    boolean existsByEmpresa(String empresa);

    List<AlunoTE> findByEmail(String email);
    List<AlunoTE> findByEmailContaining(String email);
    boolean existsByEmail(String email);

    List<AlunoTE> findByNome(String nome);
    List<AlunoTE> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

}
