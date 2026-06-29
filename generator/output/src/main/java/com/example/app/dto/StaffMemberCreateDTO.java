package com.example.app.dto;

import lombok.*;
import java.util.*;
import com.example.app.models.StaffMember;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMemberCreateDTO {

    private String department;
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

    public StaffMember toEntity() {
        StaffMember staffMember = new StaffMember();
        staffMember.setDepartment(this.getDepartment());
        staffMember.setMemberNumber(this.getMemberNumber());
        staffMember.setName(this.getName());
        staffMember.setEmail(this.getEmail());
        staffMember.setPoints(this.getPoints());
        staffMember.setActiveLoans(this.getActiveLoans());
        return staffMember;
    }
}
