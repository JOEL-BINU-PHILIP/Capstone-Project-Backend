package com.app.booking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingFeedback {
    private Integer rating;             // 1-5 stars
    private String feedback;
    private Instant ratedAt;
}
