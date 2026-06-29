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
@Table(name = "library")
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // attributes
    @Column(name = "name")
    private String name;

    // relationship: Library -> Member
    @JsonIgnoreProperties("library")
    @OneToMany(mappedBy = "library")
    @Builder.Default
    private List<Member> members = new ArrayList<>();

    // relationship: Library -> Book
    @OneToMany
    @JoinColumn(name = "library_id")
    @Builder.Default
    @MapKey(name = "isbn")
    private Map<String, Book> catalog = new HashMap<>();

}
