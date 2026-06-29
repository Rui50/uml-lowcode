package com.example.app.service;

import com.example.app.models.Book;
import com.example.app.repository.BookRepository;
import com.example.app.dto.BookCreateDTO;
import com.example.app.dto.BookResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


import com.example.app.models.Loan;
import com.example.app.repository.LoanRepository;
import com.example.app.models.Author;
import com.example.app.repository.AuthorRepository;
import com.example.app.models.Member;
import com.example.app.repository.MemberRepository;

import com.example.app.models.BookStatus;
@Service
@Transactional
@RequiredArgsConstructor
public class BookService {

    // Dependencies
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final AuthorRepository authorRepository;
    private final MemberRepository memberRepository;

   // CRUD methods
    /**
     * @return list of all entities as DTOs
     */
    public List<BookResponseDTO> findAll() {
        return bookRepository.findAll().stream()
            .map(BookResponseDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<BookResponseDTO> findById(Long id) {
        return bookRepository.findById(id)
            .map(BookResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public BookResponseDTO create(BookCreateDTO dto) {
        Book book = dto.toEntity();

        Book saved = bookRepository.save(book);

        if (dto.getLoansIds() != null) {
            for (Loan loans : loanRepository.findAllById(dto.getLoansIds())) {
                saved.getLoans().add(loans);
                loans.setBook(saved);
                loanRepository.save(loans);
            }
        }
        if (dto.getAuthorsIds() != null) {
            for (Author authors : authorRepository.findAllById(dto.getAuthorsIds())) {
                saved.getAuthors().add(authors);
            }
        }
        if (dto.getReservedByIds() != null) {
            for (Member reservedBy : memberRepository.findAllById(dto.getReservedByIds())) {
                saved.getReservedBy().add(reservedBy);
                reservedBy.getReservedBook().add(saved);
                memberRepository.save(reservedBy);
            }
        }
        if (dto.getBorrowerId() != null) {
            Member borrower = memberRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new RuntimeException("Member not found: " + dto.getBorrowerId()));
            saved.setBorrower(borrower);
        } else {
            saved.setBorrower(null);
        }
        if (dto.getEditorsIds() != null) {
            for (Author editors : authorRepository.findAllById(dto.getEditorsIds())) {
                saved.getEditors().add(editors);
            }
        }
        saved = bookRepository.save(saved);
        return BookResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public BookResponseDTO update(Long id, BookCreateDTO dto) {
        Book existing = bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found: " + id));

        // Update attributes from DTO
        existing.setIsbn(dto.getIsbn());
        existing.setTitle(dto.getTitle());
        existing.setStatus(dto.getStatus());

        if (dto.getLoansIds() != null) {
            for (Loan loans : loanRepository.findAllById(dto.getLoansIds())) {
                existing.getLoans().add(loans);
                loans.setBook(existing);
                loanRepository.save(loans);
            }
        }
        if (dto.getAuthorsIds() != null) {
            for (Author authors : authorRepository.findAllById(dto.getAuthorsIds())) {
                existing.getAuthors().add(authors);
            }
        }
        if (dto.getReservedByIds() != null) {
            for (Member reservedBy : memberRepository.findAllById(dto.getReservedByIds())) {
                existing.getReservedBy().add(reservedBy);
                reservedBy.getReservedBook().add(existing);
                memberRepository.save(reservedBy);
            }
        }
        if (dto.getBorrowerId() != null) {
            Member borrower = memberRepository.findById(dto.getBorrowerId())
                .orElseThrow(() -> new RuntimeException("Member not found: " + dto.getBorrowerId()));
            existing.setBorrower(borrower);
        } else {
            existing.setBorrower(null);
        }
        if (dto.getEditorsIds() != null) {
            for (Author editors : authorRepository.findAllById(dto.getEditorsIds())) {
                existing.getEditors().add(editors);
            }
        }
        Book saved = bookRepository.saveAndFlush(existing);
        return BookResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return bookRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToMany: Add Author entities to Book
     * @param bookId the Book id
     * @param authorIds list of Author ids to add
     * @return the updated Book
     */
    public BookResponseDTO addAuthors(Long bookId, List<Long> authorIds) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        List<Author> authors = authorRepository.findAllById(authorIds);

        for (Author author : authors) {
            book.getAuthors().add(author);
        }

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToMany: Remove Author entities from Book
     * @param bookId the Book id
     * @param authorIds list of Author ids to remove
     * @return the updated Book
     */
    public BookResponseDTO removeAuthors(Long bookId, List<Long> authorIds) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        List<Author> authors = authorRepository.findAllById(authorIds);

        for (Author author : authors) {
            book.getAuthors().remove(author);
        }

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }


    /**
     * ManyToOne: Assign a Member to Book
     * @param bookId the Book id
     * @param borrowerId the Member id
     * @return the updated Book
     */
    public BookResponseDTO assignBorrower(Long bookId, Long borrowerId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        Member borrower = memberRepository.findById(borrowerId)
            .orElseThrow(() -> new RuntimeException("Member not found: " + borrowerId));

        // Remove from previous Member if exists
        if (book.getBorrower() != null) {
            book.getBorrower().getBorrowedBooks().remove(book);
        }

        // Set both sides of the relationship
        book.setBorrower(borrower);
        borrower.getBorrowedBooks().add(book);

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Member from Book
     * @param bookId the Book id
     * @return the updated Book
     */
    public BookResponseDTO removeBorrower(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));

        // Remove from both sides
        if (book.getBorrower() != null) {
            book.getBorrower().getBorrowedBooks().remove(book);
        }
        book.setBorrower(null);

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }


    /**
     * ManyToMany: Add Author entities to Book
     * @param bookId the Book id
     * @param authorIds list of Author ids to add
     * @return the updated Book
     */
    public BookResponseDTO addEditors(Long bookId, List<Long> authorIds) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        List<Author> authors = authorRepository.findAllById(authorIds);

        for (Author author : authors) {
            book.getEditors().add(author);
        }

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToMany: Remove Author entities from Book
     * @param bookId the Book id
     * @param authorIds list of Author ids to remove
     * @return the updated Book
     */
    public BookResponseDTO removeEditors(Long bookId, List<Long> authorIds) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        List<Author> authors = authorRepository.findAllById(authorIds);

        for (Author author : authors) {
            book.getEditors().remove(author);
        }

        Book saved = bookRepository.save(book);
        return BookResponseDTO.fromEntity(saved);
    }

}
