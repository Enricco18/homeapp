package com.home.backend.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// só os campos que usamos do GET /v1/users/{id} da Backend API do Clerk
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClerkUser(
        String id,
        String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("primary_email_address_id") String primaryEmailAddressId,
        @JsonProperty("email_addresses") List<EmailAddress> emailAddresses
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmailAddress(
            String id,
            @JsonProperty("email_address") String emailAddress
    ) {
    }

    // igual ao {{user.full_name}} do token: "first last", ou null se não tiver nenhum dos dois
    public String fullName() {
        String name = Stream.of(firstName, lastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return name.isEmpty() ? null : name;
    }

    // igual ao {{user.primary_email_address}} do token
    public String primaryEmail() {
        if (emailAddresses == null) {
            return null;
        }
        return emailAddresses.stream()
                .filter(email -> email.id().equals(primaryEmailAddressId))
                .map(EmailAddress::emailAddress)
                .findFirst()
                .orElse(null);
    }
}
