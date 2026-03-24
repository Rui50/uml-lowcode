package com.example.app.service;

import com.example.app.models.Leitor;
import com.example.app.repository.LeitorRepository;

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
public class LeitorService {

    // Dependencies
    private final LeitorRepository leitorRepository;
    private final EmprestimoRepository emprestimoRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Leitor> findAll() {
        return leitorRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Leitor> findById(Long id) {
        return leitorRepository.findById(id);
    }

    /**
     * @param leitor the entity to create
     * @return the created entity
     */
    public Leitor create(Leitor leitor) {
        return leitorRepository.save(leitor);
    }

    /**
     * @param id the entity id
     * @param leitor the entity data to update
     * @return the updated entity
     */
    public Leitor update(Long id, Leitor leitor) {
        // Find existing entity
        Leitor existing = leitorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Leitor not found: " + id));

        // Update all attributes
        existing.setNome(leitor.getNome());
        existing.setNumCartao(leitor.getNumCartao());
        existing.setEmail(leitor.getEmail());


        // para ja coleções nao sao updated aqui

        return leitorRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        leitorRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return leitorRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add Emprestimo entities to Leitor
     * @param leitorId the Leitor id
     * @param emprestimoIds list of Emprestimo ids to add
     * @return the updated Leitor
     */
    public Leitor addEmprestimos(Long leitorId, List<Long> emprestimoIds) {
        Leitor leitor = leitorRepository.findById(leitorId)
            .orElseThrow(() -> new RuntimeException("Leitor not found: " + leitorId));
        List<Emprestimo> emprestimos = emprestimoRepository.findAllById(emprestimoIds);

        leitor.getEmprestimos().addAll(emprestimos);

        return leitorRepository.save(leitor);
    }

    /**
     * OneToMany (unidirectional): Remove Emprestimo entities from Leitor
     * @param leitorId the Leitor id
     * @param emprestimoIds list of Emprestimo ids to remove
     * @return the updated Leitor
     */
    public Leitor removeEmprestimos(Long leitorId, List<Long> emprestimoIds) {
        Leitor leitor = leitorRepository.findById(leitorId)
            .orElseThrow(() -> new RuntimeException("Leitor not found: " + leitorId));

        leitor.getEmprestimos().removeIf(emprestimo -> emprestimoIds.contains(emprestimo.getId()));

        return leitorRepository.save(leitor);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return boolean
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public boolean estaAtivo(Long id) {
        throw new UnsupportedOperationException(
            "Method 'estaAtivo' not yet implemented. "
        );
    }

}
