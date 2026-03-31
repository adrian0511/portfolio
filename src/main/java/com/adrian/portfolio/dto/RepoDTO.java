package com.adrian.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepoDTO {
    private String name;
    private String description;
    private String html_url;
    private String language;
    private String topic;
}
