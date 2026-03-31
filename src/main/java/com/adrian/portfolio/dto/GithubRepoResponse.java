package com.adrian.portfolio.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepoResponse {

    private String name;
    private String description;
    private String html_url;
    private String language;
    private Boolean fork;
    private List<String> topics;

}
