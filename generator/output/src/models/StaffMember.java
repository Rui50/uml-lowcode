package models;

import java.util.*;
import java.time.*;

public class StaffMember extends Member {

    private String department;

    public StaffMember() {
    }

    public StaffMember(int memberNumber, String name, String email, int points, int activeLoans, String department) {
        super(memberNumber, name, email, points, activeLoans);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return "StaffMember{" +
            "id=" + getId() +
            ", department='" + getDepartment() + "'" +
            ", memberNumber=" + getMemberNumber() +
            ", name='" + getName() + "'" +
            ", email='" + getEmail() + "'" +
            ", points=" + getPoints() +
            ", activeLoans=" + getActiveLoans() +
            "}";
    }

}
