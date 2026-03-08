package com.example.app.service;

import com.example.app.models.Turma;
import com.example.app.repository.TurmaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


import com.example.app.models.Aluno;
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

    // Simple query methods for strings
    /**
     * Find Turma by nome
     * @param nome to search by
     * @return list of matching entities
     */
    public List<Turma> findByNome(String nome) {
        return turmaRepository.findByNome(nome);
    }

    /**
     * Find Turma by nome containing text
     * @param nome to search
     * @return list of matching entities
     */
    public List<Turma> findByNomeContaining(String nome) {
        return turmaRepository.findByNomeContaining(nome);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @param alunos List<Aluno>
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public void inicializarAlunosParaTeste(Long id, List<Aluno> alunos) {
        throw new UnsupportedOperationException(
            "Method 'inicializarAlunosParaTeste' not yet implemented. "
        );
    }

    /**
     * @param destino Turma
     * @param numero int
     * @return boolean
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public boolean transferirAluno(Long id, Turma destino, int numero) {
        throw new UnsupportedOperationException(
            "Method 'transferirAluno' not yet implemented. "
        );
    }

    /**
     * @return int
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public int totalAlunos(Long id) {
        throw new UnsupportedOperationException(
            "Method 'totalAlunos' not yet implemented. "
        );
    }

}
