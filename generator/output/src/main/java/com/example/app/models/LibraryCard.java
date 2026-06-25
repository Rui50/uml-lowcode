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
@Table(name = "librarycard")
public class LibraryCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "cardNumber")
    private String cardNumber;

    @Column(name = "issuedDay")
    private int issuedDay;

    // relationship: LibraryCard -> Member
    @JsonIgnoreProperties("card")
    @OneToOne(mappedBy = "card")
    private Member member;

}
