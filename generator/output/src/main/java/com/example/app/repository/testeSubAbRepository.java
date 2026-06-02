package com.example.app.repository;

import com.example.app.models.testeSubAb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for testeSubAb entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface testeSubAbRepository extends JpaRepository<testeSubAb, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<testeSubAb> findByNada(String nada);
    List<testeSubAb> findByNadaContaining(String nada);
    boolean existsByNada(String nada);

    List<testeSubAb> findByTestar(String testar);
    List<testeSubAb> findByTestarContaining(String testar);
    boolean existsByTestar(String testar);

}
