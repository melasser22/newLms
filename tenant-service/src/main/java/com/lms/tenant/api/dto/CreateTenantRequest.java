package com.lms.tenant.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new tenant.
 *
 * @param slug The unique, URL-friendly identifier for the tenant. Must be lowercase, alphanumeric, with dashes.
 * @param name The display name of the tenant.
 */
public record CreateTenantRequest(
    @NotBlank(message = "Slug cannot be blank.")
    @Size(min = 3, max = 63, message = "Slug must be between 3 and 63 characters.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase, alphanumeric, and can contain hyphens.")
    String slug,

    @NotBlank(message = "Name cannot be blank.")
    @Size(max = 255, message = "Name cannot be longer than 255 characters.")
    String name
) {
}
