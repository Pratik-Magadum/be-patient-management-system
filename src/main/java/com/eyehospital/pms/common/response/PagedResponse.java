package com.eyehospital.pms.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagination metadata + content wrapper for list endpoints.
 * Returned as the {@code data} field inside {@link ApiResponse} for paginated collections.
 *
 * @param <T> type of items in the page
 */
@Getter
@Builder
@Schema(description = "Paginated response with metadata")
public class PagedResponse<T> {

    @Schema(description = "Items in the current page")
    private final List<T> content;

    @Schema(description = "Zero-based page number", example = "0")
    private final int page;

    @Schema(description = "Number of items per page", example = "20")
    private final int size;

    @Schema(description = "Total number of items across all pages", example = "150")
    private final long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private final int totalPages;

    @Schema(description = "Whether this is the last page", example = "false")
    private final boolean last;

    /**
     * Builds a {@code PagedResponse} directly from a Spring {@link Page} slice.
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
