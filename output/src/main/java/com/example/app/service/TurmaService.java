package com.example.app.service;

import com.example.app.models.Turma;
import com.example.app.repository.TurmaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class TurmaService {

    // Dependencies
    private final TurmaRepository turmaRepository;

    /**
     * @return list of all entities
     */
    public List<Turma> findAll() {
        return turmaRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Turma> findById(Long id) {
        return turmaRepository.findById(id);
    }

    /**
     * @param turma the entity to save
     * @return the saved entity
     */
    public Turma save(Turma turma) {
        // TODO: Add validation logic here (OCL constraints)
        return turmaRepository.save(turma);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        turmaRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return turmaRepository.existsById(id);
    }

}
