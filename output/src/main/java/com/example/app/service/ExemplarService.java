package com.example.app.service;

import com.example.app.models.Exemplar;
import com.example.app.repository.ExemplarRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Emprestimo;
import com.example.app.repository.EmprestimoRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class ExemplarService {

    // Dependencies
    private final ExemplarRepository exemplarRepository;
    private final EmprestimoRepository emprestimoRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Exemplar> findAll() {
        return exemplarRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Exemplar> findById(Long id) {
        return exemplarRepository.findById(id);
    }

    /**
     * @param exemplar the entity to create
     * @return the created entity
     */
    public Exemplar create(Exemplar exemplar) {
        return exemplarRepository.save(exemplar);
    }

    /**
     * @param id the entity id
     * @param exemplar the entity data to update
     * @return the updated entity
     */
    public Exemplar update(Long id, Exemplar exemplar) {
        // Find existing entity
        Exemplar existing = exemplarRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exemplar not found: " + id));

        // Update all attributes
        existing.setCodigo(exemplar.getCodigo());
        existing.setDisponivel(exemplar.isDisponivel());


        // para ja coleções nao sao updated aqui

        return exemplarRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        exemplarRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return exemplarRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add Emprestimo entities to Exemplar
     * @param exemplarId the Exemplar id
     * @param emprestimoIds list of Emprestimo ids to add
     * @return the updated Exemplar
     */
    public Exemplar addEmprestimos(Long exemplarId, List<Long> emprestimoIds) {
        Exemplar exemplar = exemplarRepository.findById(exemplarId)
            .orElseThrow(() -> new RuntimeException("Exemplar not found: " + exemplarId));
        List<Emprestimo> emprestimos = emprestimoRepository.findAllById(emprestimoIds);

        exemplar.getEmprestimos().addAll(emprestimos);

        return exemplarRepository.save(exemplar);
    }

    /**
     * OneToMany (unidirectional): Remove Emprestimo entities from Exemplar
     * @param exemplarId the Exemplar id
     * @param emprestimoIds list of Emprestimo ids to remove
     * @return the updated Exemplar
     */
    public Exemplar removeEmprestimos(Long exemplarId, List<Long> emprestimoIds) {
        Exemplar exemplar = exemplarRepository.findById(exemplarId)
            .orElseThrow(() -> new RuntimeException("Exemplar not found: " + exemplarId));

        exemplar.getEmprestimos().removeIf(emprestimo -> emprestimoIds.contains(emprestimo.getId()));

        return exemplarRepository.save(exemplar);
    }

}
