package com.example.app.service;

import com.example.app.models.LibraryCard;
import com.example.app.repository.LibraryCardRepository;
import com.example.app.dto.LibraryCardCreateDTO;
import com.example.app.dto.LibraryCardResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * @return list of all entities as DTOs
     */
    public List<LibraryCardResponseDTO> findAll() {
        return libraryCardRepository.findAll().stream()
            .map(LibraryCardResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
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
                .orElseThrow(() -> new RuntimeException("Member not found: " + dto.getMemberId()));
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
            .orElseThrow(() -> new RuntimeException("LibraryCard not found: " + id));

        // Update attributes from DTO
        existing.setCardNumber(dto.getCardNumber());
        existing.setIssuedDay(dto.getIssuedDay());

        if (dto.getMemberId() != null) {
            Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found: " + dto.getMemberId()));
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
    public boolean existsById(Long id) {
        return libraryCardRepository.existsById(id);
    }

}
