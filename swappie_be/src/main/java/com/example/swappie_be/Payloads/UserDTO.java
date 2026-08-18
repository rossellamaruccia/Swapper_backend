package com.example.swappie_be.Payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDTO(
        String name,
        String surname,
        String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email,
        String password,
        String city,
        LocationDTO location) {
}
