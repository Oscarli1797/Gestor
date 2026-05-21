package com.GestorProyectos.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GitHub contribution statistics for the past year. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContributionStatsDto implements Serializable {
    private int totalCommits;
    private int totalPullRequests;
    private int totalIssues;
    private int totalPrReviews;
    private int totalContributions; // calendar total (all types)
    private int last90DaysCommits;
    private int last30DaysCommits;
}
