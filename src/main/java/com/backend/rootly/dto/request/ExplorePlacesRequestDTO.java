package com.backend.rootly.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
// Use field defaults for omitted JSON properties instead of the all-args constructor.
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
public class ExplorePlacesRequestDTO {

    @NotNull(message = "q must not be null")
    @Size(max = 120, message = "q must be at most 120 characters")
    private String q = "";

    @NotNull(message = "category must not be null")
    private String category = "all";

    @Min(value = 0, message = "page must be 0..1000000")
    @Max(value = 1_000_000, message = "page must be 0..1000000")
    private int page;

    @Min(value = 1, message = "size must be 1..50")
    @Max(value = 50, message = "size must be 1..50")
    private int size = 20;
}
