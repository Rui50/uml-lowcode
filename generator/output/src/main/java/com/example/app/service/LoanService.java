package com.example.app.service;

import com.example.app.models.Loan;
import com.example.app.repository.LoanRepository;
import com.example.app.dto.LoanCreateDTO;
import com.example.app.dto.LoanResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.app.exception.NotFoundException;

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
public class LoanService {

    // Dependencies
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;

   // CRUD methods
    /**
     * @param pageable paginação/ordenação (query params: page, size, sort)
     * @return page of entities as DTOs
     */
    public Page<LoanResponseDTO> findAll(Pageable pageable) {
        return loanRepository.findAll(pageable)
            .map(LoanResponseDTO::fromEntity);
    }

    /**
     * @param id the entity id
     * @return Optional containing the entity DTO if found
     */
    public Optional<LoanResponseDTO> findById(Long id) {
        return loanRepository.findById(id)
            .map(LoanResponseDTO::fromEntity);
    }

    /**
     * @param dto the data to create the entity
     * @return the created entity as DTO
     */
    public LoanResponseDTO create(LoanCreateDTO dto) {
        Loan loan = dto.toEntity();

        Loan saved = loanRepository.save(loan);

        if (dto.getMemberId() != null) {
            Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new NotFoundException("Member not found: " + dto.getMemberId()));
            saved.setMember(member);
        } else {
            saved.setMember(null);
        }
        if (dto.getBookId() != null) {
            Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found: " + dto.getBookId()));
            saved.setBook(book);
        } else {
            saved.setBook(null);
        }
        saved = loanRepository.save(saved);
        return LoanResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     * @param dto the data to update
     * @return the updated entity as DTO
     */
    public LoanResponseDTO update(Long id, LoanCreateDTO dto) {
        Loan existing = loanRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + id));

        // Update attributes from DTO
        existing.setLoanDay(dto.getLoanDay());
        existing.setDueDay(dto.getDueDay());
        existing.setReturned(dto.getReturned());

        if (dto.getMemberId() != null) {
            Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new NotFoundException("Member not found: " + dto.getMemberId()));
            existing.setMember(member);
        } else {
            existing.setMember(null);
        }
        if (dto.getBookId() != null) {
            Book book = bookRepository.findById(dto.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found: " + dto.getBookId()));
            existing.setBook(book);
        } else {
            existing.setBook(null);
        }
        Loan saved = loanRepository.saveAndFlush(existing);
        return LoanResponseDTO.fromEntity(saved);
    }

    /**
     * @param id the entity id
     */
    public void deleteById(Long id) {
        loanRepository.deleteById(id);
    }

    /**
     * @param id the entity id
     * @return true if exists
     */
    public boolean existsById(Long id) {
        return loanRepository.existsById(id);
    }

    // Relationship stuff

    /**
     * ManyToOne: Assign a Member to Loan
     * @param loanId the Loan id
     * @param memberId the Member id
     * @return the updated Loan
     */
    public LoanResponseDTO assignMember(Long loanId, Long memberId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("Member not found: " + memberId));

        // Remove from previous Member if exists
        if (loan.getMember() != null) {
            loan.getMember().getLoans().remove(loan);
        }

        // Set both sides of the relationship
        loan.setMember(member);
        member.getLoans().add(loan);

        Loan saved = loanRepository.save(loan);
        return LoanResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Member from Loan
     * @param loanId the Loan id
     * @return the updated Loan
     */
    public LoanResponseDTO removeMember(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));

        // Remove from both sides
        if (loan.getMember() != null) {
            loan.getMember().getLoans().remove(loan);
        }
        loan.setMember(null);

        Loan saved = loanRepository.save(loan);
        return LoanResponseDTO.fromEntity(saved);
    }


    /**
     * ManyToOne: Assign a Book to Loan
     * @param loanId the Loan id
     * @param bookId the Book id
     * @return the updated Loan
     */
    public LoanResponseDTO assignBook(Long loanId, Long bookId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new NotFoundException("Book not found: " + bookId));

        // Remove from previous Book if exists
        if (loan.getBook() != null) {
            loan.getBook().getLoans().remove(loan);
        }

        // Set both sides of the relationship
        loan.setBook(book);
        book.getLoans().add(loan);

        Loan saved = loanRepository.save(loan);
        return LoanResponseDTO.fromEntity(saved);
    }

    /**
     * ManyToOne: Remove the Book from Loan
     * @param loanId the Loan id
     * @return the updated Loan
     */
    public LoanResponseDTO removeBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));

        // Remove from both sides
        if (loan.getBook() != null) {
            loan.getBook().getLoans().remove(loan);
        }
        loan.setBook(null);

        Loan saved = loanRepository.save(loan);
        return LoanResponseDTO.fromEntity(saved);
    }

    // operations (TODO: implement)

    public void returnLoan() {
        throw new UnsupportedOperationException(
            "Method 'returnLoan' not yet implemented. "
        );
    }
}
