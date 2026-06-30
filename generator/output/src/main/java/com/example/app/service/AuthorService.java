package com.example.app.service;

import com.example.app.models.Author;
import com.example.app.repository.AuthorRepository;
import com.example.app.dto.AuthorCreateDTO;
import com.example.app.dto.AuthorResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.app.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.BookStatus;
@Service
@Transactional
@RequiredArgsConstructor
public class AuthorService {

    // Dependencies
    private final AuthorRepository authorRepository;

   // CRUD methods
    /**
     * @param pageable paginação/ordenação (query params: page, size, sort)
     * @return page of entities as DTOs
     */
    @Transactional(readOnly = true)
    public Page<AuthorResponseDTO> findAll(Pageable pageable) {
        return authorRepository.findAll(pageable)
            .map(AuthorResponseDTO::fromEntity);
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<AuthorResponseDTO> findById(Long id) {
        return authorRepository.findById(id)
            .map(AuthorResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public AuthorResponseDTO create(AuthorCreateDTO dto) {
        Author author = dto.toEntity();

        Author saved = authorRepository.save(author);

        return AuthorResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public AuthorResponseDTO update(Long id, AuthorCreateDTO dto) {
        Author existing = authorRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Author not found: " + id));

        // Update attributes from DTO
        existing.setName(dto.getName());

        Author saved = authorRepository.saveAndFlush(existing);
        return AuthorResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        authorRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return authorRepository.existsById(id);
    }

}
