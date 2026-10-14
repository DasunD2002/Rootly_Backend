package com.backend.rootly.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
public class ProvinceExploreRequestDTO {
    @NotBlank(message = "provinceId is required")
    @Size(max = 32, message = "provinceId must be at most 32 characters")
    private String provinceId;

    @NotNull(message = "q must not be null")
    @Size(max = 120, message = "q must be at most 120 characters")
    private String q = "";

    @NotNull(message = "page must not be null")
    @Min(value = 0, message = "page must be 0..1000000")
    @Max(value = 1_000_000, message = "page must be 0..1000000")
    private Integer page = 0;

    @NotNull(message = "size must not be null")
    @Min(value = 1, message = "size must be 1..50")
    @Max(value = 50, message = "size must be 1..50")
    private Integer size = 10;
}
