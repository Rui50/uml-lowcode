package com.example.app.service;

import com.example.app.models.Library;
import com.example.app.repository.LibraryRepository;
import com.example.app.dto.LibraryCreateDTO;
import com.example.app.dto.LibraryResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Book;
import com.example.app.repository.BookRepository;
import com.example.app.models.Member;
import com.example.app.repository.MemberRepository;

import com.example.app.models.BookStatus;
@Service
@Transactional
@RequiredArgsConstructor
public class LibraryService {

    // Dependencies
    private final LibraryRepository libraryRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<LibraryResponseDTO> findAll() {
        return libraryRepository.findAll().stream()
            .map(LibraryResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<LibraryResponseDTO> findById(Long id) {
        return libraryRepository.findById(id)
            .map(LibraryResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public LibraryResponseDTO create(LibraryCreateDTO dto) {
        Library library = dto.toEntity();

        Library saved = libraryRepository.save(library);

        if (dto.getMembersIds() != null) {
            for (Member members : memberRepository.findAllById(dto.getMembersIds())) {
                saved.getMembers().add(members);
                members.setLibrary(saved);
                memberRepository.save(members);
            }
        }
        if (dto.getCatalogIds() != null) {
            for (Book catalog : bookRepository.findAllById(dto.getCatalogIds())) {
                saved.getCatalog().put(catalog.getIsbn(), catalog);
            }
        }
        saved = libraryRepository.save(saved);
        return LibraryResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public LibraryResponseDTO update(Long id, LibraryCreateDTO dto) {
        Library existing = libraryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Library not found: " + id));

        // Update attributes from DTO
        existing.setName(dto.getName());

        if (dto.getMembersIds() != null) {
            for (Member members : memberRepository.findAllById(dto.getMembersIds())) {
                existing.getMembers().add(members);
                members.setLibrary(existing);
                memberRepository.save(members);
            }
        }
        if (dto.getCatalogIds() != null) {
            for (Book catalog : bookRepository.findAllById(dto.getCatalogIds())) {
                existing.getCatalog().put(catalog.getIsbn(), catalog);
            }
        }
        Library saved = libraryRepository.saveAndFlush(existing);
        return LibraryResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        libraryRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return libraryRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * OneToMany (unidirectional): Add Book entities to Library
     * @param libraryId the Library id
     * @param bookIds list of Book ids to add
     * @return the updated Library
     */
    public LibraryResponseDTO addCatalog(Long libraryId, List<Long> bookIds) {
        Library library = libraryRepository.findById(libraryId)
            .orElseThrow(() -> new RuntimeException("Library not found: " + libraryId));
        List<Book> books = bookRepository.findAllById(bookIds);

        for (Book book : books) {
            library.getCatalog().put(book.getIsbn(), book);
        }

        Library saved = libraryRepository.save(library);
        return LibraryResponseDTO.fromEntity(saved);
    }

    /**
     * OneToMany (unidirectional): Remove Book entities from Library
     * @param libraryId the Library id
     * @param bookIds list of Book ids to remove
     * @return the updated Library
     */
    public LibraryResponseDTO removeCatalog(Long libraryId, List<Long> bookIds) {
        Library library = libraryRepository.findById(libraryId)
            .orElseThrow(() -> new RuntimeException("Library not found: " + libraryId));

        List<Book> books = bookRepository.findAllById(bookIds);
        for (Book book : books) {
            library.getCatalog().remove(book.getIsbn());
        }

        Library saved = libraryRepository.save(library);
        return LibraryResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public void markOverdue(int currentDay) {
        throw new UnsupportedOperationException(
            "Method 'markOverdue' not yet implemented. "
        );
    }
}
