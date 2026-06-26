package com.example.app.repository;

import com.example.app.models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Categoria entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Categoria> findByNome(String nome);
    List<Categoria> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

    List<Categoria> findByDescricao(String descricao);
    List<Categoria> findByDescricaoContaining(String descricao);
    boolean existsByDescricao(String descricao);

}
