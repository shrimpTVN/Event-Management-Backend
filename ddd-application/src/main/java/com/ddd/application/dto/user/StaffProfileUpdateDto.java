package com.ddd.application.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StaffProfileUpdateDto {
    private String firstName;
    private String lastName;
    private String gender;
    private StaffProfileDetailsUpdateDto staffProfile;
    private boolean firstNamePresent;
    private boolean lastNamePresent;
    private boolean genderPresent;
    private boolean staffProfilePresent;

    public StaffProfileUpdateDto(String firstName, String lastName, String gender, StaffProfileDetailsUpdateDto staffProfile,
                                 boolean firstNamePresent, boolean lastNamePresent, boolean genderPresent, boolean staffProfilePresent) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.staffProfile = staffProfile;
        this.firstNamePresent = firstNamePresent;
        this.lastNamePresent = lastNamePresent;
        this.genderPresent = genderPresent;
        this.staffProfilePresent = staffProfilePresent;
    }
}
