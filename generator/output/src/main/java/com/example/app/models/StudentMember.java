package com.example.app.models;

import jakarta.persistence.*;
import java.util.*;
import java.time.*;

@Entity
public class StudentMember extends Member {

    // attributes
    @Column(name = "course")
    private String course;

    @Column(name = "maxLoans")
    private int maxLoans;


    public StudentMember() {}

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public int getMaxLoans() { return maxLoans; }
    public void setMaxLoans(int maxLoans) { this.maxLoans = maxLoans; }

    @Override
    public String toString() {
        return "StudentMember{" + "id=" + getId() + ", course=" + getCourse() + ", maxLoans=" + getMaxLoans() + ", memberNumber=" + getMemberNumber() + ", name=" + getName() + ", email=" + getEmail() + ", points=" + getPoints() + ", activeLoans=" + getActiveLoans() + "}";
    }
}
