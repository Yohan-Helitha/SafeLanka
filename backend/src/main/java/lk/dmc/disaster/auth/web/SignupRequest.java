package lk.dmc.disaster.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lk.dmc.disaster.auth.application.SignupService.SignupCommand;
import lk.dmc.disaster.auth.domain.Nic;
import lk.dmc.disaster.auth.domain.PasswordPolicy;
import lk.dmc.disaster.auth.domain.PhoneNumber;

public record SignupRequest(
    @NotBlank(message = "Enter your full name.")
        @Size(min = 2, max = 120, message = "Your name must be 2 to 120 characters.")
        String fullName,
    @NotBlank(message = "Enter your mobile number.")
        @Pattern(
            regexp = PhoneNumber.PATTERN,
            message = "Enter a Sri Lankan mobile number, for example 077 123 4567.")
        String phone,
    @Email(message = "Enter a valid email address.")
        @Size(max = 120, message = "The email can be at most 120 characters.")
        String email,
    @NotBlank(message = "Enter your NIC number.")
        @Pattern(regexp = Nic.PATTERN, message = "Enter a valid NIC: 9 digits and V, or 12 digits.")
        String nic,
    @NotBlank(message = "Enter your home address.")
        @Size(min = 5, max = 200, message = "The address must be 5 to 200 characters.")
        String homeAddress,
    @NotNull(message = "Choose your district.") UUID districtId,
    @NotBlank(message = "Choose a language.")
        @Pattern(regexp = "^(en|si|ta)$", message = "Choose English, Sinhala or Tamil.")
        String preferredLanguage,
    @NotBlank(message = "Choose a password.")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String password) {

  SignupCommand toCommand() {
    return new SignupCommand(
        fullName, phone, email, nic, homeAddress, districtId, preferredLanguage, password);
  }
}
