package com.example.app.jobs;

import com.example.app.models.Library;
import com.example.app.models.Member;
import com.example.app.models.StudentMember;
import com.example.app.models.StaffMember;
import com.example.app.models.Book;
import com.example.app.models.Author;
import com.example.app.models.Loan;
import com.example.app.models.LibraryCard;
import com.example.app.models.BookStatus;
import com.example.app.repository.LoanRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional
public class MarkOverdueJob {

    private final LoanRepository loanRepository;

    @Scheduled(cron = "0 * * * * *")
    public void run() {
        List<Loan> loans = loanRepository.findAll();
        for (Loan loan : loans) {
            if (!loan.getReturned()) {
                Member member = loan.getMember();
                member.setPoints(member.getPoints() - 1);
            }
        }
    }
}
