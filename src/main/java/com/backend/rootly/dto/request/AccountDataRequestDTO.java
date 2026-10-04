package com.backend.rootly.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record AccountDataRequestDTO(@Min(0) long version, @NotNull Map<String, Object> data) {
}
