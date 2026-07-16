package models;

import java.util.*;
import java.time.*;

public class StudentMember extends Member {

    private String course;
    private int maxLoans;

    public StudentMember() {
    }

    public StudentMember(int memberNumber, String name, String email, int points, int activeLoans, String course, int maxLoans) {
        super(memberNumber, name, email, points, activeLoans);
        this.course = course;
        this.maxLoans = maxLoans;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public int getMaxLoans() {
        return maxLoans;
    }

    public void setMaxLoans(int maxLoans) {
        this.maxLoans = maxLoans;
    }

    @Override
    public String toString() {
        return "StudentMember{" +
            "id=" + getId() +
            ", course='" + getCourse() + "'" +
            ", maxLoans=" + getMaxLoans() +
            ", memberNumber=" + getMemberNumber() +
            ", name='" + getName() + "'" +
            ", email='" + getEmail() + "'" +
            ", points=" + getPoints() +
            ", activeLoans=" + getActiveLoans() +
            "}";
    }

}
