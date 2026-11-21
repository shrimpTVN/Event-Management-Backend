package com.ddd.application.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class StaffProfileDetailsUpdateDto {
    private String title;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private boolean titlePresent;
    private boolean dateOfBirthPresent;
    private boolean phoneNumberPresent;

    public StaffProfileDetailsUpdateDto(String title, LocalDate dateOfBirth, String phoneNumber,
                                        boolean titlePresent, boolean dateOfBirthPresent, boolean phoneNumberPresent) {
        this.title = title;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.titlePresent = titlePresent;
        this.dateOfBirthPresent = dateOfBirthPresent;
        this.phoneNumberPresent = phoneNumberPresent;
    }
}
