package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Library;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryCreateDTO {

    private String name;
    private List<Long> membersIds;
    private List<Long> catalogIds;

    public Library toEntity() {
        Library library = new Library();
        library.setName(this.getName());
        return library;
    }
}
