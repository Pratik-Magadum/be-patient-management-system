package com.eyehospital.pms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard API response envelope used for every endpoint.
 *
 * <pre>{@code
 * {
 *   "status"    : 200,
 *   "message"   : "Hospital retrieved successfully",
 *   "data"      : { ... },
 *   "timestamp" : "2026-03-03T10:15:30"
 * }
 * }</pre>
 *
 * @param <T> type of the payload carried in {@code data}
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiResponse<T> {

    @Schema(description = "HTTP status code", example = "200")
    private final int status;

    @Schema(description = "Human-readable result message", example = "Hospital retrieved successfully")
    private final String message;

    @Schema(description = "Response payload – null on error responses")
    private final T data;

    @Schema(description = "Server timestamp when the response was produced")
    private final LocalDateTime timestamp;

    /**
     * Creates a successful response with a payload.
     */
    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response without a payload.
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
