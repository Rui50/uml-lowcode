package com.example.app.dto;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;
import com.example.app.models.Library;
import com.example.app.models.Member;
import com.example.app.models.Book;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryResponseDTO {

    private Long id;

    private String name;

    private List<Long> membersIds;
    private List<Long> catalogIds;

    public static LibraryResponseDTO fromEntity(Library library) {
        LibraryResponseDTO dto = new LibraryResponseDTO();
        dto.setId(library.getId());
        dto.setName(library.getName());
        dto.setMembersIds(library.getMembers() != null
            ? library.getMembers().stream().map(Member::getId).collect(Collectors.toList())
            : new ArrayList<>());
        dto.setCatalogIds(library.getCatalog() != null
            ? library.getCatalog().values().stream().map(Book::getId).collect(Collectors.toList())
            : new ArrayList<>());
        return dto;
    }
}
