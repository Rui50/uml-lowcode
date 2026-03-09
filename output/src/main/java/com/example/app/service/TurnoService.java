package com.example.app.service;

import com.example.app.models.Turno;
import com.example.app.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class TurnoService {

    // Dependencies
    private final TurnoRepository turnoRepository;

    /**
     * @return list of all entities
     */
    public List<Turno> findAll() {
        return turnoRepository.findAll();
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity if found
     */
    public Optional<Turno> findById(Long id) {
        return turnoRepository.findById(id);
    }

    /**
     * @param turno the entity to save
     * @return the saved entity
     */
    public Turno save(Turno turno) {
        // ocl validation
        List<String> errors = turno.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                "Validation failed: " + String.join(", ", errors)
            );
        }

        return turnoRepository.save(turno);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        turnoRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return turnoRepository.existsById(id);
    }

    // Custom operations from class diagram (TODO: implement)
    /**
     * @return boolean
     * @throws UnsupportedOperationException until implemented with Sequence Diagram
     */
    public boolean temVaga(Long id) {
        throw new UnsupportedOperationException(
            "Method 'temVaga' not yet implemented. "
        );
    }

}
