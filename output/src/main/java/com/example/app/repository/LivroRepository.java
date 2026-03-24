package com.example.app.repository;

import com.example.app.models.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Livro entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface LivroRepository extends JpaRepository<Livro, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Livro> findByIsbn(String isbn);
    List<Livro> findByIsbnContaining(String isbn);
    boolean existsByIsbn(String isbn);

    List<Livro> findByTitulo(String titulo);
    List<Livro> findByTituloContaining(String titulo);
    boolean existsByTitulo(String titulo);

}
