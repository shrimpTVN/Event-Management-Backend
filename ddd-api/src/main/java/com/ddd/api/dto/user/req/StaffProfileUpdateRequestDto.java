package com.ddd.api.dto.user.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StaffProfileUpdateRequestDto {

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Valid
    private StaffProfileDetailsUpdateRequestDto staffProfile;

    private boolean firstNamePresent;
    private boolean lastNamePresent;
    private boolean genderPresent;
    private boolean staffProfilePresent;

    public StaffProfileUpdateRequestDto(String firstName, String lastName, String gender, StaffProfileDetailsUpdateRequestDto staffProfile) {
        setFirstName(firstName);
        setLastName(lastName);
        setGender(gender);
        setStaffProfile(staffProfile);
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.firstNamePresent = true;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.lastNamePresent = true;
    }

    public void setGender(String gender) {
        this.gender = gender;
        this.genderPresent = true;
    }

    public void setStaffProfile(StaffProfileDetailsUpdateRequestDto staffProfile) {
        this.staffProfile = staffProfile;
        this.staffProfilePresent = true;
    }
}
