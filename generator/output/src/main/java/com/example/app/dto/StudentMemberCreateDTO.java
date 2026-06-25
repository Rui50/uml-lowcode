package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.StudentMember;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMemberCreateDTO {

    private String course;
    private int maxLoans;
    private int memberNumber;
    private String name;
    private String email;
    private int points;
    private int activeLoans;
    private Long libraryId;
    private List<Long> loansIds;
    private List<Long> reservedBookIds;
    private List<Long> borrowedBooksIds;
    private Long cardId;

    public StudentMember toEntity() {
        StudentMember studentMember = new StudentMember();
        studentMember.setCourse(this.getCourse());
        studentMember.setMaxLoans(this.getMaxLoans());
        studentMember.setMemberNumber(this.getMemberNumber());
        studentMember.setName(this.getName());
        studentMember.setEmail(this.getEmail());
        studentMember.setPoints(this.getPoints());
        studentMember.setActiveLoans(this.getActiveLoans());
        return studentMember;
    }
}
