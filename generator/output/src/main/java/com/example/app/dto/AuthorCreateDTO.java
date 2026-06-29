package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Author;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorCreateDTO {

    private String name;

    public Author toEntity() {
        Author author = new Author();
        author.setName(this.getName());
        return author;
    }
}
