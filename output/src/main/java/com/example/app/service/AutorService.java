package com.example.app.service;

import com.example.app.models.Autor;
import com.example.app.repository.AutorRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;


import com.example.app.models.Livro;
import com.example.app.repository.LivroRepository;
@Service
@Transactional
@RequiredArgsConstructor
public class AutorService {

    // Dependencies
    private final AutorRepository autorRepository;
    private final LivroRepository livroRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<Autor> findAll() {
        return autorRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Autor> findById(Long id) {
        return autorRepository.findById(id);
    }

    /**
     * @param autor the entity to create
     * @return the created entity
     */
    public Autor create(Autor autor) {
        return autorRepository.save(autor);
    }

    /**
     * @param id the entity id
     * @param autor the entity data to update
     * @return the updated entity
     */
    public Autor update(Long id, Autor autor) {
        // Find existing entity
        Autor existing = autorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Autor not found: " + id));

        // Update all attributes
        existing.setNacionlidade(autor.getNacionlidade());
        existing.setNome(autor.getNome());


        // para ja coleções nao sao updated aqui

        return autorRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        autorRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return autorRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToMany: Add Livro entities to Autor
     * @param autorId the Autor id
     * @param livroIds list of Livro ids to add
     * @return the updated Autor
     */
    public Autor addLivros(Long autorId, List<Long> livroIds) {
        Autor autor = autorRepository.findById(autorId)
            .orElseThrow(() -> new RuntimeException("Autor not found: " + autorId));
        List<Livro> livros = livroRepository.findAllById(livroIds);

        for (Livro livro : livros) {
            autor.getLivros().add(livro);
            livro.getAutores().add(autor);
        }

        return autorRepository.save(autor);
    }

    /**
     * ManyToMany: Remove Livro entities from Autor
     * @param autorId the Autor id
     * @param livroIds list of Livro ids to remove
     * @return the updated Autor
     */
    public Autor removeLivros(Long autorId, List<Long> livroIds) {
        Autor autor = autorRepository.findById(autorId)
            .orElseThrow(() -> new RuntimeException("Autor not found: " + autorId));
        List<Livro> livros = livroRepository.findAllById(livroIds);

        for (Livro livro : livros) {
            autor.getLivros().remove(livro);
            livro.getAutores().remove(autor);
        }

        return autorRepository.save(autor);
    }

}
