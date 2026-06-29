package com.example.app.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "loanDay")
    private int loanDay;

    @Column(name = "dueDay")
    private int dueDay;

    @Column(name = "returned")
    private boolean returned;

    // relationship: Loan -> Member
    @JsonIgnoreProperties("loans")
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    // relationship: Loan -> Book
    @JsonIgnoreProperties("loans")
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;

}
