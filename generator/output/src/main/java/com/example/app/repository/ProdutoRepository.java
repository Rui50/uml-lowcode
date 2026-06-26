package com.example.app.repository;

import com.example.app.models.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Produto entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Produto> findByNome(String nome);
    List<Produto> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

}
