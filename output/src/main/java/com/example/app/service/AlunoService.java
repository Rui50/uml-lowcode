package com.example.app.service;

import com.example.app.models.Aluno;
import com.example.app.repository.AlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
     * @param aluno the entity to save
     * @return the saved entity
     */
    public Aluno save(Aluno aluno) {
        // TODO: Add validation logic here (OCL constraints)
        return alunoRepository.save(aluno);
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

}
