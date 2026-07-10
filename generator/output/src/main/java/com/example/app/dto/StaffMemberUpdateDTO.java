package com.example.app.dto;

import lombok.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMemberUpdateDTO {

    private String department;
    private Integer memberNumber;
    private String name;
    private String email;
    private Integer points;
    private Integer activeLoans;
    private Long libraryId;
    private Long cardId;
}
