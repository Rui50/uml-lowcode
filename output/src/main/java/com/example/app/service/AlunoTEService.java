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
    private final AlunoTERepository alunoTERepository;

   // CRUD methods
    /**
     * @return list of all entities
     */
    public List<AlunoTE> findAll() {
        return alunoTERepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<AlunoTE> findById(Long id) {
        return alunoTERepository.findById(id);
    }

    /**
     * @param alunoTE the entity to create
     * @return the created entity
     */
    public AlunoTE create(AlunoTE alunoTE) {
        return alunoTERepository.save(alunoTE);
    }

    /**
     * @param id the entity id
     * @param alunoTE the entity data to update
     * @return the updated entity
     */
    public AlunoTE update(Long id, AlunoTE alunoTE) {
        // Find existing entity
        AlunoTE existing = alunoTERepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AlunoTE not found: " + id));

        // Update all attributes
        existing.setEmpresa(alunoTE.getEmpresa());


        // para ja coleções nao sao updated aqui

        return alunoTERepository.save(existing);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        alunoTERepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return alunoTERepository.existsById(id);
    }

}
