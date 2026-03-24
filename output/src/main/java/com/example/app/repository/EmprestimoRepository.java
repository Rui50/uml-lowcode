package com.example.app.repository;

import com.example.app.models.Emprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Emprestimo entity
 * Spring Data JPA generates a base implementation automatically
 */
@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    // Automatic CRUD methods provided by JpaRepository:
    // - save(entity)
    // - findById(id)
    // - findAll()
    // - deleteById(id)
    // - count()
    // - existsById(id)

    // Query methods (Spring generates implementation):
    List<Emprestimo> findByDataEmprestimo(String dataEmprestimo);
    List<Emprestimo> findByDataEmprestimoContaining(String dataEmprestimo);
    boolean existsByDataEmprestimo(String dataEmprestimo);

    List<Emprestimo> findByDataDevolucao(String dataDevolucao);
    List<Emprestimo> findByDataDevolucaoContaining(String dataDevolucao);
    boolean existsByDataDevolucao(String dataDevolucao);

}
