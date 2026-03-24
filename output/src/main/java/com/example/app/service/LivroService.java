package com.example.app.service;

import com.example.app.models.Livro;
import com.example.app.repository.LivroRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Exemplar;
import com.example.app.repository.ExemplarRepository;
import com.example.app.models.Autor;
import com.example.app.repository.AutorRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class LivroService {

    // Dependencies
    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final AutorRepository autorRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Livro> findAll() {
        return livroRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Livro> findById(Long id) {
        return livroRepository.findById(id);
    }

    /**
     * @param livro the entity to create
     * @return the created entity
     */
    public Livro create(Livro livro) {
        return livroRepository.save(livro);
    }

    /**
     * @param id the entity id
     * @param livro the entity data to update
     * @return the updated entity
     */
    public Livro update(Long id, Livro livro) {
        // Find existing entity
        Livro existing = livroRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Livro not found: " + id));

        // Update all attributes
        existing.setIsbn(livro.getIsbn());
        existing.setTitulo(livro.getTitulo());
        existing.setAnoPublicacao(livro.getAnoPublicacao());


        // para ja coleções nao sao updated aqui

        return livroRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        livroRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return livroRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add Exemplar entities to Livro
     * @param livroId the Livro id
     * @param exemplarIds list of Exemplar ids to add
     * @return the updated Livro
     */
    public Livro addExemplars(Long livroId, List<Long> exemplarIds) {
        Livro livro = livroRepository.findById(livroId)
            .orElseThrow(() -> new RuntimeException("Livro not found: " + livroId));
        List<Exemplar> exemplars = exemplarRepository.findAllById(exemplarIds);

        livro.getExemplares().addAll(exemplars);

        return livroRepository.save(livro);
    }

    /**
     * OneToMany (unidirectional): Remove Exemplar entities from Livro
     * @param livroId the Livro id
     * @param exemplarIds list of Exemplar ids to remove
     * @return the updated Livro
     */
    public Livro removeExemplars(Long livroId, List<Long> exemplarIds) {
        Livro livro = livroRepository.findById(livroId)
            .orElseThrow(() -> new RuntimeException("Livro not found: " + livroId));

        livro.getExemplares().removeIf(exemplar -> exemplarIds.contains(exemplar.getId()));

        return livroRepository.save(livro);
    }

}
