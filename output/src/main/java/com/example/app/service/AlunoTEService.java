package com.example.app.service;

import com.example.app.models.AlunoTE;
import com.example.app.repository.AlunoTERepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AlunoTEService {

    // Dependencies
    private final AlunoTERepository alunoteRepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<AlunoTE> findAll() {
        return alunoteRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<AlunoTE> findById(Long id) {
        return alunoteRepository.findById(id);
    }

    /**
     * @param alunote the entity to create
     * @return the created entity
     */
    public AlunoTE create(AlunoTE alunote) {
        return alunoteRepository.save(alunote);
    }

    /**
     * @param id the entity id
     * @param alunote the entity data to update
     * @return the updated entity
     */
    public AlunoTE update(Long id, AlunoTE alunote) {
        // Find existing entity
        AlunoTE existing = alunoteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AlunoTE not found: " + id));

        // Update all attributes
        existing.setEmpresa(alunote.getEmpresa());


        // para ja coleções nao sao updated aqui

        return alunoteRepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        alunoteRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return alunoteRepository.existsById(id);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return boolean
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public boolean comprovativo(Long id) {
        throw new UnsupportedOperationException(
            "Method 'comprovativo' not yet implemented. "
        );
    }

}
