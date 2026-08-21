package com.adrian.portfolio.dto;

import java.util.List;

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
    private List<String> topics;
    private String pushed_at;
}
