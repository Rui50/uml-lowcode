package com.example.app.service;

import com.example.app.models.StaffMember;
import com.example.app.repository.StaffMemberRepository;
import com.example.app.dto.StaffMemberCreateDTO;
import com.example.app.dto.StaffMemberResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.app.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.LibraryCard;
import com.example.app.repository.LibraryCardRepository;
import com.example.app.models.Loan;
import com.example.app.repository.LoanRepository;
import com.example.app.models.Book;
import com.example.app.repository.BookRepository;
import com.example.app.models.Library;
import com.example.app.repository.LibraryRepository;
import com.example.app.models.Member;
import com.example.app.repository.MemberRepository;

import com.example.app.models.BookStatus;
@Service
@Transactional
@RequiredArgsConstructor
public class StaffMemberService {

    // Dependencies
    private final StaffMemberRepository staffMemberRepository;
    private final LibraryRepository libraryRepository;
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final LibraryCardRepository libraryCardRepository;

   // CRUD methods
    /**
     * @param pageable paginação/ordenação (query params: page, size, sort)
     * @return page of entities as DTOs
     */
    public Page<StaffMemberResponseDTO> findAll(Pageable pageable) {
        return staffMemberRepository.findAll(pageable)
            .map(StaffMemberResponseDTO::fromEntity);
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<StaffMemberResponseDTO> findById(Long id) {
        return staffMemberRepository.findById(id)
            .map(StaffMemberResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public StaffMemberResponseDTO create(StaffMemberCreateDTO dto) {
        StaffMember staffMember = dto.toEntity();

        StaffMember saved = staffMemberRepository.save(staffMember);

        if (dto.getLibraryId() != null) {
            Library library = libraryRepository.findById(dto.getLibraryId())
                .orElseThrow(() -> new NotFoundException("Library not found: " + dto.getLibraryId()));
            saved.setLibrary(library);
        } else {
            saved.setLibrary(null);
        }
        if (dto.getLoansIds() != null) {
            for (Loan loans : loanRepository.findAllById(dto.getLoansIds())) {
                saved.getLoans().add(loans);
                loans.setMember(saved);
                loanRepository.save(loans);
            }
        }
        if (dto.getReservedBookIds() != null) {
            for (Book reservedBook : bookRepository.findAllById(dto.getReservedBookIds())) {
                saved.getReservedBook().add(reservedBook);
            }
        }
        if (dto.getBorrowedBooksIds() != null) {
            for (Book borrowedBooks : bookRepository.findAllById(dto.getBorrowedBooksIds())) {
                saved.getBorrowedBooks().add(borrowedBooks);
                borrowedBooks.setBorrower(saved);
                bookRepository.save(borrowedBooks);
            }
        }
        if (dto.getCardId() != null) {
            LibraryCard card = libraryCardRepository.findById(dto.getCardId())
                .orElseThrow(() -> new NotFoundException("LibraryCard not found: " + dto.getCardId()));
            saved.setCard(card);
        } else {
            saved.setCard(null);
        }
        saved = staffMemberRepository.save(saved);
        return StaffMemberResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public StaffMemberResponseDTO update(Long id, StaffMemberCreateDTO dto) {
        StaffMember existing = staffMemberRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("StaffMember not found: " + id));

        // Update attributes from DTO
        existing.setDepartment(dto.getDepartment());
        existing.setMemberNumber(dto.getMemberNumber());
        existing.setName(dto.getName());
        existing.setEmail(dto.getEmail());
        existing.setPoints(dto.getPoints());
        existing.setActiveLoans(dto.getActiveLoans());

        if (dto.getLibraryId() != null) {
            Library library = libraryRepository.findById(dto.getLibraryId())
                .orElseThrow(() -> new NotFoundException("Library not found: " + dto.getLibraryId()));
            existing.setLibrary(library);
        } else {
            existing.setLibrary(null);
        }
        if (dto.getLoansIds() != null) {
            for (Loan loans : new ArrayList<>(existing.getLoans())) {
                loans.setMember(null);
                loanRepository.save(loans);
            }
            existing.getLoans().clear();
            for (Loan loans : loanRepository.findAllById(dto.getLoansIds())) {
                existing.getLoans().add(loans);
                loans.setMember(existing);
                loanRepository.save(loans);
            }
        }
        if (dto.getReservedBookIds() != null) {
            for (Book reservedBook : new ArrayList<>(existing.getReservedBook())) {
                reservedBook.getReservedBy().remove(existing);
            }
            existing.getReservedBook().clear();
            for (Book reservedBook : bookRepository.findAllById(dto.getReservedBookIds())) {
                existing.getReservedBook().add(reservedBook);
            }
        }
        if (dto.getBorrowedBooksIds() != null) {
            for (Book borrowedBooks : new ArrayList<>(existing.getBorrowedBooks())) {
                borrowedBooks.setBorrower(null);
                bookRepository.save(borrowedBooks);
            }
            existing.getBorrowedBooks().clear();
            for (Book borrowedBooks : bookRepository.findAllById(dto.getBorrowedBooksIds())) {
                existing.getBorrowedBooks().add(borrowedBooks);
                borrowedBooks.setBorrower(existing);
                bookRepository.save(borrowedBooks);
            }
        }
        if (dto.getCardId() != null) {
            LibraryCard card = libraryCardRepository.findById(dto.getCardId())
                .orElseThrow(() -> new NotFoundException("LibraryCard not found: " + dto.getCardId()));
            existing.setCard(card);
        } else {
            existing.setCard(null);
        }
        StaffMember saved = staffMemberRepository.saveAndFlush(existing);
        return StaffMemberResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        staffMemberRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return staffMemberRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToOne: Assign a Library to StaffMember
     * @param staffMemberId the StaffMember id
     * @param libraryId the Library id
     * @return the updated StaffMember
     */
    public StaffMemberResponseDTO assignLibrary(Long staffMemberId, Long libraryId) {
        StaffMember staffMember = staffMemberRepository.findById(staffMemberId)
            .orElseThrow(() -> new NotFoundException("StaffMember not found: " + staffMemberId));
        Library library = libraryRepository.findById(libraryId)
            .orElseThrow(() -> new NotFoundException("Library not found: " + libraryId));

        // Remove from previous Library if exists
        if (staffMember.getLibrary() != null) {
            staffMember.getLibrary().getMembers().remove(staffMember);
        }

        // Set both sides of the relationship
        staffMember.setLibrary(library);
        library.getMembers().add(staffMember);

        StaffMember saved = staffMemberRepository.save(staffMember);
        return StaffMemberResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Library from StaffMember
     * @param staffMemberId the StaffMember id
     * @return the updated StaffMember
     */
    public StaffMemberResponseDTO removeLibrary(Long staffMemberId) {
        StaffMember staffMember = staffMemberRepository.findById(staffMemberId)
            .orElseThrow(() -> new NotFoundException("StaffMember not found: " + staffMemberId));

        // Remove from both sides
        if (staffMember.getLibrary() != null) {
            staffMember.getLibrary().getMembers().remove(staffMember);
        }
        staffMember.setLibrary(null);

        StaffMember saved = staffMemberRepository.save(staffMember);
        return StaffMemberResponseDTO.fromEntity(saved);
    }


    /**
     * ManyToMany: Add Book entities to StaffMember
     * @param staffMemberId the StaffMember id
     * @param bookIds list of Book ids to add
     * @return the updated StaffMember
     */
    public StaffMemberResponseDTO addReservedBook(Long staffMemberId, List<Long> bookIds) {
        StaffMember staffMember = staffMemberRepository.findById(staffMemberId)
            .orElseThrow(() -> new NotFoundException("StaffMember not found: " + staffMemberId));
        List<Book> books = bookRepository.findAllById(bookIds);

        for (Book book : books) {
            staffMember.getReservedBook().add(book);
            book.getReservedBy().add(staffMember);
        }

        StaffMember saved = staffMemberRepository.save(staffMember);
        return StaffMemberResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToMany: Remove Book entities from StaffMember
     * @param staffMemberId the StaffMember id
     * @param bookIds list of Book ids to remove
     * @return the updated StaffMember
     */
    public StaffMemberResponseDTO removeReservedBook(Long staffMemberId, List<Long> bookIds) {
        StaffMember staffMember = staffMemberRepository.findById(staffMemberId)
            .orElseThrow(() -> new NotFoundException("StaffMember not found: " + staffMemberId));
        List<Book> books = bookRepository.findAllById(bookIds);

        for (Book book : books) {
            staffMember.getReservedBook().remove(book);
            book.getReservedBy().remove(staffMember);
        }

        StaffMember saved = staffMemberRepository.save(staffMember);
        return StaffMemberResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public void redeemPoints(int memberId, int ammount) {
        throw new UnsupportedOperationException(
            "Method 'redeemPoints' not yet implemented. "
        );
    }

    public Loan borrowBook(Book book, int today) {
        throw new UnsupportedOperationException(
            "Method 'borrowBook' not yet implemented. "
        );
    }
}
