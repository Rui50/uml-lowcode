package com.example.app.repository;

import com.example.app.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Book entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Book> findByIsbn(String isbn);
    List<Book> findByIsbnContaining(String isbn);
    boolean existsByIsbn(String isbn);

    List<Book> findByTitle(String title);
    List<Book> findByTitleContaining(String title);
    boolean existsByTitle(String title);

}
