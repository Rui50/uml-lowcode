package com.example.app.service;

import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AlunoService {

    // Dependencies
    private final AlunoRepository alunoRepository;

    /**
     * @return list of all entities
     */
    public List<Aluno> findAll() {
        return alunoRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Aluno> findById(Long id) {
        return alunoRepository.findById(id);
    }

    /**
     * @param aluno the entity to create
     * @return the created entity
     */
    public Aluno create(Aluno aluno) {
        return alunoRepository.save(aluno);
    }

    /**
     * @param id the entity id
     * @param aluno the entity data to update
     * @return the updated entity
     */
    public Aluno update(Long id, Aluno aluno) {
        // Find existing entity
        Aluno existing = alunoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Aluno not found: " + id));

        // Update all attributes
        existing.setNome(aluno.getNome());
        existing.setEmail(aluno.getEmail());
        existing.setNumero(aluno.getNumero());


        // para ja coleções nao sao updated aqui

        return alunoRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        alunoRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return alunoRepository.existsById(id);
    }

    // Simple query methods for strings
    /**
     * Find Aluno by nome
     * @param nome to search by
     * @return list of matching entities
     */
    public List<Aluno> findByNome(String nome) {
        return alunoRepository.findByNome(nome);
    }

    /**
     * Find Aluno by nome containing text
     * @param nome to search
     * @return list of matching entities
     */
    public List<Aluno> findByNomeContaining(String nome) {
        return alunoRepository.findByNomeContaining(nome);
    }

    /**
     * Find Aluno by email
     * @param email to search by
     * @return list of matching entities
     */
    public List<Aluno> findByEmail(String email) {
        return alunoRepository.findByEmail(email);
    }

    /**
     * Find Aluno by email containing text
     * @param email to search
     * @return list of matching entities
     */
    public List<Aluno> findByEmailContaining(String email) {
        return alunoRepository.findByEmailContaining(email);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return List<String>
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public List<String> validar(Long id) {
        throw new UnsupportedOperationException(
            "Method 'validar' not yet implemented. "
        );
    }

}
