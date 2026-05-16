package com.mycelis.model.dto.requests;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for creating a new monitoring target.
 * Validated at the controller layer before reaching business logic.
 */
@Data
public class CreateStalkRequest {
    @NotBlank(message = "URL is required")
    @Size(max = 2048)
    private String url;

    @Size(max = 255, message = "Nickname cannot be longer than 255 characters")
    private String nickname;

    @NotNull(message = "Check interval is required")
    @Min(value = 10, message = "Minimum interval is 10 seconds")
    @Max(value = 86400, message = "Maximum interval is 86400 seconds")
    private Integer growthIntervalSeconds;

    @NotNull(message = "Timeout is required")
    @Min(value = 5, message = "Minimum timeout is 5 seconds")
    @Max(value = 120, message = "Maximum timeout is 120 seconds")
    private Integer timeoutSeconds;
}
