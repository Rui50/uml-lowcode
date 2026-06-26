package com.example.app.controllers;

import com.example.app.models.Cliente;
import com.example.app.service.ClienteService;
import com.example.app.dto.ClienteCreateDTO;
import com.example.app.dto.ClienteResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.app.models.Encomenda;

/**
 * REST Controller for Cliente
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // CRUD Endpoints

    /**
     * GET /api/clientes
     * Retrieve all Cliente entities
     * @return list of all entities
     */
    @GetMapping
    public ResponseEntity<List<ClienteResponseDTO>> getAll() {
        List<ClienteResponseDTO> entities = clienteService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * GET /api/clientes/{id}
     * Retrieve Cliente by ID
     * @param id entity id
     * @return entity if found, 404 if not
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> getById(@PathVariable Long id) {
        return clienteService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/clientes
     * Create new Cliente
     * @param dto entity data
     * @return created entity
     */
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> create(@RequestBody ClienteCreateDTO dto) {
        ClienteResponseDTO created = clienteService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> update(
            @PathVariable Long id,
            @RequestBody ClienteCreateDTO dto) {
        ClienteResponseDTO updated = clienteService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/clientes/{id}
     * Delete Cliente
     * @param id entity id
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!clienteService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        clienteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Relationship Endpoints

    /**
     * Add Encomenda entities to Cliente by IDs
     */
    @PostMapping("/{id}/encomendas")
    public ResponseEntity<ClienteResponseDTO> addEncomendas(
            @PathVariable Long id,
            @RequestBody List<Long> encomendaIds) {
        ClienteResponseDTO updated = clienteService.addEncomendas(id, encomendaIds);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove Encomenda entities from Cliente by IDs
     */
    @DeleteMapping("/{id}/encomendas")
    public ResponseEntity<ClienteResponseDTO> removeEncomendas(
            @PathVariable Long id,
            @RequestBody List<Long> encomendaIds) {
        ClienteResponseDTO updated = clienteService.removeEncomendas(id, encomendaIds);
        return ResponseEntity.ok(updated);
    }

}
