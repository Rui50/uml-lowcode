package com.example.app.repository;

import com.example.app.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Cliente entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Cliente> findByNome(String nome);
    List<Cliente> findByNomeContaining(String nome);
    boolean existsByNome(String nome);

    List<Cliente> findByEmail(String email);
    List<Cliente> findByEmailContaining(String email);
    boolean existsByEmail(String email);

}
