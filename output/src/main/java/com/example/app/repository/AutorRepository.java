package com.example.app.repository;

import com.example.app.models.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Autor entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface AutorRepository extends JpaRepository<Autor, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Autor> findByNacionlidade(String nacionlidade);
    List<Autor> findByNacionlidadeContaining(String nacionlidade);
    boolean existsByNacionlidade(String nacionlidade);

    List<Autor> findByNome(String nome);
    List<Autor> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

}
