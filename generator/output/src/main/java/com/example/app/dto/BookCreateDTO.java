package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.Book;
import com.example.app.models.BookStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCreateDTO {

    private String isbn;
    private String title;
    private BookStatus status;
    private List<Long> loansIds;
    private List<Long> authorsIds;
    private List<Long> reservedByIds;
    private Long borrowerId;
    private List<Long> editorsIds;

    public Book toEntity() {
        Book book = new Book();
        book.setIsbn(this.getIsbn());
        book.setTitle(this.getTitle());
        book.setStatus(this.getStatus());
        return book;
    }
}
