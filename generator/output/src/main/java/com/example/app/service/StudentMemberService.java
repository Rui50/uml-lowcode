package com.example.app.service;

import com.example.app.models.StudentMember;
import com.example.app.repository.StudentMemberRepository;
import com.example.app.dto.StudentMemberCreateDTO;
import com.example.app.dto.StudentMemberResponseDTO;

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
public class StudentMemberService {

    // Dependencies
    private final StudentMemberRepository studentMemberRepository;
    private final LibraryRepository libraryRepository;
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final LibraryCardRepository libraryCardRepository;

   // CRUD methods
    /**
     * @param pageable paginação/ordenação (query params: page, size, sort)
     * @return page of entities as DTOs
     */
    public Page<StudentMemberResponseDTO> findAll(Pageable pageable) {
        return studentMemberRepository.findAll(pageable)
            .map(StudentMemberResponseDTO::fromEntity);
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<StudentMemberResponseDTO> findById(Long id) {
        return studentMemberRepository.findById(id)
            .map(StudentMemberResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public StudentMemberResponseDTO create(StudentMemberCreateDTO dto) {
        StudentMember studentMember = dto.toEntity();

        StudentMember saved = studentMemberRepository.save(studentMember);

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
        saved = studentMemberRepository.save(saved);
        return StudentMemberResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public StudentMemberResponseDTO update(Long id, StudentMemberCreateDTO dto) {
        StudentMember existing = studentMemberRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("StudentMember not found: " + id));

        // Update attributes from DTO
        existing.setCourse(dto.getCourse());
        existing.setMaxLoans(dto.getMaxLoans());
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
        StudentMember saved = studentMemberRepository.saveAndFlush(existing);
        return StudentMemberResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        studentMemberRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return studentMemberRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToOne: Assign a Library to StudentMember
     * @param studentMemberId the StudentMember id
     * @param libraryId the Library id
     * @return the updated StudentMember
     */
    public StudentMemberResponseDTO assignLibrary(Long studentMemberId, Long libraryId) {
        StudentMember studentMember = studentMemberRepository.findById(studentMemberId)
            .orElseThrow(() -> new NotFoundException("StudentMember not found: " + studentMemberId));
        Library library = libraryRepository.findById(libraryId)
            .orElseThrow(() -> new NotFoundException("Library not found: " + libraryId));

        // Remove from previous Library if exists
        if (studentMember.getLibrary() != null) {
            studentMember.getLibrary().getMembers().remove(studentMember);
        }

        // Set both sides of the relationship
        studentMember.setLibrary(library);
        library.getMembers().add(studentMember);

        StudentMember saved = studentMemberRepository.save(studentMember);
        return StudentMemberResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Library from StudentMember
     * @param studentMemberId the StudentMember id
     * @return the updated StudentMember
     */
    public StudentMemberResponseDTO removeLibrary(Long studentMemberId) {
        StudentMember studentMember = studentMemberRepository.findById(studentMemberId)
            .orElseThrow(() -> new NotFoundException("StudentMember not found: " + studentMemberId));

        // Remove from both sides
        if (studentMember.getLibrary() != null) {
            studentMember.getLibrary().getMembers().remove(studentMember);
        }
        studentMember.setLibrary(null);

        StudentMember saved = studentMemberRepository.save(studentMember);
        return StudentMemberResponseDTO.fromEntity(saved);
    }


    /**
     * ManyToMany: Add Book entities to StudentMember
     * @param studentMemberId the StudentMember id
     * @param bookIds list of Book ids to add
     * @return the updated StudentMember
     */
    public StudentMemberResponseDTO addReservedBook(Long studentMemberId, List<Long> bookIds) {
        StudentMember studentMember = studentMemberRepository.findById(studentMemberId)
            .orElseThrow(() -> new NotFoundException("StudentMember not found: " + studentMemberId));
        List<Book> books = bookRepository.findAllById(bookIds);

        for (Book book : books) {
            studentMember.getReservedBook().add(book);
            book.getReservedBy().add(studentMember);
        }

        StudentMember saved = studentMemberRepository.save(studentMember);
        return StudentMemberResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToMany: Remove Book entities from StudentMember
     * @param studentMemberId the StudentMember id
     * @param bookIds list of Book ids to remove
     * @return the updated StudentMember
     */
    public StudentMemberResponseDTO removeReservedBook(Long studentMemberId, List<Long> bookIds) {
        StudentMember studentMember = studentMemberRepository.findById(studentMemberId)
            .orElseThrow(() -> new NotFoundException("StudentMember not found: " + studentMemberId));
        List<Book> books = bookRepository.findAllById(bookIds);

        for (Book book : books) {
            studentMember.getReservedBook().remove(book);
            book.getReservedBy().remove(studentMember);
        }

        StudentMember saved = studentMemberRepository.save(studentMember);
        return StudentMemberResponseDTO.fromEntity(saved);
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
