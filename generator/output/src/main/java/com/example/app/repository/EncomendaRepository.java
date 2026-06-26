package com.example.app.repository;

import com.example.app.models.Encomenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Encomenda entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface EncomendaRepository extends JpaRepository<Encomenda, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Encomenda> findByData(String data);
    List<Encomenda> findByDataContaining(String data);
    boolean existsByData(String data);

    List<Encomenda> findByEstado(String estado);
    List<Encomenda> findByEstadoContaining(String estado);
    boolean existsByEstado(String estado);

}
