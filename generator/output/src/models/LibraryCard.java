package models;

import java.util.*;
import java.time.*;

public class LibraryCard {

    private Long id;

    private String cardNumber;
    private int issuedDay;

    private Member member;
    public LibraryCard() {
    }

    public LibraryCard(String cardNumber, int issuedDay) {
        this.cardNumber = cardNumber;
        this.issuedDay = issuedDay;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public int getIssuedDay() {
        return issuedDay;
    }

    public void setIssuedDay(int issuedDay) {
        this.issuedDay = issuedDay;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    @Override
    public String toString() {
        return "LibraryCard{" +
            "id=" + getId() +
            ", cardNumber='" + getCardNumber() + "'" +
            ", issuedDay=" + getIssuedDay() +
            "}";
    }

}
