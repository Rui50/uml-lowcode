package com.example.app.service;

import com.example.app.models.AlunoTE;
import com.example.app.repository.AlunoTERepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AlunoTEService {

    // Dependencies
    private final AlunoTERepository alunoteRepository;

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
     * @param alunote the entity to save
     * @return the saved entity
     */
    public AlunoTE save(AlunoTE alunote) {
        // TODO: Add validation logic here (OCL constraints)
        return alunoteRepository.save(alunote);
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

}
