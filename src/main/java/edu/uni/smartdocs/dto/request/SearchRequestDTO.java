package edu.uni.smartdocs.dto.request;

import lombok.Data;

@Data
public class SearchRequestDTO {
    private String query;
    private double threshold = 0.7;
    private int limit = 5;

    // Getters and Setters (nếu không dùng Lombok)
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}