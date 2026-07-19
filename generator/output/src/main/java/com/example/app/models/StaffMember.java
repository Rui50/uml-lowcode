package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
public class StaffMember extends Member {

    // attributes
    @Column(name = "department")
    private String department;


    public StaffMember() {}

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return "StaffMember{" + "id=" + getId() + ", department=" + getDepartment() + ", memberNumber=" + getMemberNumber() + ", name=" + getName() + ", email=" + getEmail() + ", points=" + getPoints() + ", activeLoans=" + getActiveLoans() + "}";
    }
}
