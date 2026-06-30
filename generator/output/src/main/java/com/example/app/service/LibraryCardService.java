package com.example.app.service;

import com.example.app.models.LibraryCard;
import com.example.app.repository.LibraryCardRepository;
import com.example.app.dto.LibraryCardCreateDTO;
import com.example.app.dto.LibraryCardResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.app.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Member;
import com.example.app.repository.MemberRepository;

import com.example.app.models.BookStatus;
@Service
@Transactional
@RequiredArgsConstructor
public class LibraryCardService {

    // Dependencies
    private final LibraryCardRepository libraryCardRepository;
    private final MemberRepository memberRepository;

   // CRUD methods
    /**
     * @param pageable paginação/ordenação (query params: page, size, sort)
     * @return page of entities as DTOs
     */
    @Transactional(readOnly = true)
    public Page<LibraryCardResponseDTO> findAll(Pageable pageable) {
        return libraryCardRepository.findAll(pageable)
            .map(LibraryCardResponseDTO::fromEntity);
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<LibraryCardResponseDTO> findById(Long id) {
        return libraryCardRepository.findById(id)
            .map(LibraryCardResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public LibraryCardResponseDTO create(LibraryCardCreateDTO dto) {
        LibraryCard libraryCard = dto.toEntity();

        LibraryCard saved = libraryCardRepository.save(libraryCard);

        if (dto.getMemberId() != null) {
            Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new NotFoundException("Member not found: " + dto.getMemberId()));
            saved.setMember(member);
            member.setCard(saved);
            memberRepository.save(member);
        } else {
            if (saved.getMember() != null) {
                saved.getMember().setCard(null);
                memberRepository.save(saved.getMember());
            }
            saved.setMember(null);
        }
        return LibraryCardResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public LibraryCardResponseDTO update(Long id, LibraryCardCreateDTO dto) {
        LibraryCard existing = libraryCardRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("LibraryCard not found: " + id));

        // Update attributes from DTO
        existing.setCardNumber(dto.getCardNumber());
        existing.setIssuedDay(dto.getIssuedDay());

        if (dto.getMemberId() != null) {
            Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new NotFoundException("Member not found: " + dto.getMemberId()));
            existing.setMember(member);
            member.setCard(existing);
            memberRepository.save(member);
        } else {
            if (existing.getMember() != null) {
                existing.getMember().setCard(null);
                memberRepository.save(existing.getMember());
            }
            existing.setMember(null);
        }
        LibraryCard saved = libraryCardRepository.saveAndFlush(existing);
        return LibraryCardResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        libraryCardRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return libraryCardRepository.existsById(id);
    }

}
