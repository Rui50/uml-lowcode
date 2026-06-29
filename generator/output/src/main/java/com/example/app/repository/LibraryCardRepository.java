package com.example.app.repository;

import com.example.app.models.LibraryCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LibraryCard entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface LibraryCardRepository extends JpaRepository<LibraryCard, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<LibraryCard> findByCardNumber(String cardNumber);
    List<LibraryCard> findByCardNumberContaining(String cardNumber);
    boolean existsByCardNumber(String cardNumber);

}
