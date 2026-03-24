package com.example.app.service;

import com.example.app.models.Emprestimo;
import com.example.app.repository.EmprestimoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmprestimoService {

    // Dependencies
    private final EmprestimoRepository emprestimoRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Emprestimo> findAll() {
        return emprestimoRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Emprestimo> findById(Long id) {
        return emprestimoRepository.findById(id);
    }

    /**
     * @param emprestimo the entity to create
     * @return the created entity
     */
    public Emprestimo create(Emprestimo emprestimo) {
        return emprestimoRepository.save(emprestimo);
    }

    /**
     * @param id the entity id
     * @param emprestimo the entity data to update
     * @return the updated entity
     */
    public Emprestimo update(Long id, Emprestimo emprestimo) {
        // Find existing entity
        Emprestimo existing = emprestimoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Emprestimo not found: " + id));

        // Update all attributes
        existing.setDataEmprestimo(emprestimo.getDataEmprestimo());
        existing.setDataDevolucao(emprestimo.getDataDevolucao());
        existing.setDevolvido(emprestimo.isDevolvido());


        // para ja coleções nao sao updated aqui

        return emprestimoRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        emprestimoRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return emprestimoRepository.existsById(id);
    }

}
