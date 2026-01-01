package com.app.booking.dto. response;

import lombok.AllArgsConstructor;
import lombok. Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSatisfactionDTO {

    // Overall rating
    private double avgRating;
    private long totalRatings;

    // Rating distribution
    private long fiveStarCount;
    private long fourStarCount;
    private long threeStarCount;
    private long twoStarCount;
    private long oneStarCount;

    // Percentages
    private double fiveStarPercentage;
    private double fourStarPercentage;
    private double threeStarPercentage;
    private double twoStarPercentage;
    private double oneStarPercentage;

    // Rating by category
    private Map<String, Double> avgRatingByCategory;

    // Satisfaction rate (4+ stars)
    private double satisfactionRate;
}