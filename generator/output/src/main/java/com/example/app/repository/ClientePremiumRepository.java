package com.example.app.repository;

import com.example.app.models.ClientePremium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ClientePremium entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface ClientePremiumRepository extends JpaRepository<ClientePremium, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<ClientePremium> findByNome(String nome);
    List<ClientePremium> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

    List<ClientePremium> findByEmail(String email);
    List<ClientePremium> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
