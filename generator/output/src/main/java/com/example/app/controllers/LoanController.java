package com.example.app.controllers;

import com.example.app.models.Loan;
import com.example.app.service.LoanService;
import com.example.app.dto.LoanCreateDTO;
import com.example.app.dto.LoanResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.Book;
import com.example.app.models.Member;

/**
 * REST Controller for Loan
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    // CRUD Endpoints

    /**
     * GET /api/loans
     * Retrieve all Loan entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<LoanResponseDTO>> getAll() {
        List<LoanResponseDTO> entities = loanService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/loans/{id}
     * Retrieve Loan by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> getById(@PathVariable Long id) {
        return loanService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/loans
     * Create new Loan
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<LoanResponseDTO> create(@RequestBody LoanCreateDTO dto) {
        LoanResponseDTO created = loanService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanResponseDTO> update(
            @PathVariable Long id,
            @RequestBody LoanCreateDTO dto) {
        LoanResponseDTO updated = loanService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/loans/{id}
     * Delete Loan
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!loanService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        loanService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * ManyToOne: Assign a Member to Loan
     */
    @PutMapping("/{id}/member/{memberId}")
    public ResponseEntity<LoanResponseDTO> assignMember(
            @PathVariable Long id,
            @PathVariable Long memberId) {
        LoanResponseDTO updated = loanService.assignMember(id, memberId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Member from Loan
     */
    @DeleteMapping("/{id}/member")
    public ResponseEntity<LoanResponseDTO> removeMember(
            @PathVariable Long id) {
        LoanResponseDTO updated = loanService.removeMember(id);
        return ResponseEntity.ok(updated);
    }


    /**
     * ManyToOne: Assign a Book to Loan
     */
    @PutMapping("/{id}/book/{bookId}")
    public ResponseEntity<LoanResponseDTO> assignBook(
            @PathVariable Long id,
            @PathVariable Long bookId) {
        LoanResponseDTO updated = loanService.assignBook(id, bookId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ManyToOne: Remove the Book from Loan
     */
    @DeleteMapping("/{id}/book")
    public ResponseEntity<LoanResponseDTO> removeBook(
            @PathVariable Long id) {
        LoanResponseDTO updated = loanService.removeBook(id);
        return ResponseEntity.ok(updated);
    }


    // Custom Operation Endpoints dos diagramas sequencia
}
