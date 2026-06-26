package com.example.app.controllers;

import com.example.app.models.ClientePremium;
import com.example.app.service.ClientePremiumService;
import com.example.app.dto.ClientePremiumCreateDTO;
import com.example.app.dto.ClientePremiumResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for ClientePremium
 */
@RestController
@RequestMapping("/api/clientepremiums")
@RequiredArgsConstructor
public class ClientePremiumController {

    private final ClientePremiumService clientePremiumService;

    // CRUD Endpoints

    /**
     * GET /api/clientepremiums
     * Retrieve all ClientePremium entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<ClientePremiumResponseDTO>> getAll() {
        List<ClientePremiumResponseDTO> entities = clientePremiumService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/clientepremiums/{id}
     * Retrieve ClientePremium by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientePremiumResponseDTO> getById(@PathVariable Long id) {
        return clientePremiumService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/clientepremiums
     * Create new ClientePremium
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<ClientePremiumResponseDTO> create(@RequestBody ClientePremiumCreateDTO dto) {
        ClientePremiumResponseDTO created = clientePremiumService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientePremiumResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ClientePremiumCreateDTO dto) {
        ClientePremiumResponseDTO updated = clientePremiumService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/clientepremiums/{id}
     * Delete ClientePremium
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!clientePremiumService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        clientePremiumService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
