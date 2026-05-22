package com.GestorProyectos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {
    private String plan;           // "free" | "pro"
    private int    searchCount;    // searches performed this month
    private int    searchLimit;    // 200 for free, -1 = unlimited
    private long   candidateCount; // total candidates saved
    private int    candidateLimit; // 30 for free, -1 = unlimited
}
