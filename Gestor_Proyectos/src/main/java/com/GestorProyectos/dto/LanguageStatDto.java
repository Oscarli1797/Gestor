package com.GestorProyectos.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageStatDto implements Serializable {
    private String name;
    private String color;   // hex color from GitHub, e.g. "#3572A5"
    private long bytes;
    private double percentage; // 0.0–100.0, one decimal
}
