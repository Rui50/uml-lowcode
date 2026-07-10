package com.example.app.dto;

import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanUpdateDTO {

    private Integer loanDay;
    private Integer dueDay;
    private Boolean returned;
    private Long memberId;
    private Long bookId;
}
