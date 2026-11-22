package com.ddd.api.dto.user.req;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class StaffProfileDetailsUpdateRequestDto {

    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    private boolean titlePresent;
    private boolean dateOfBirthPresent;
    private boolean phoneNumberPresent;

    public StaffProfileDetailsUpdateRequestDto(String title, LocalDate dateOfBirth, String phoneNumber) {
        setTitle(title);
        setDateOfBirth(dateOfBirth);
        setPhoneNumber(phoneNumber);
    }

    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.dateOfBirthPresent = true;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.phoneNumberPresent = true;
    }
}
