package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.BookStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookUpdateDTO {

    private String isbn;
    private String title;
    private BookStatus status;
}
