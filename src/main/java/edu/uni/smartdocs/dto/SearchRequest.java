package edu.uni.smartdocs.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private Double threshold;
    private Integer limit;
}