package edu.uni.smartdocs.models;

import lombok.Data;

@Data
public class MessageDTO {
    private Long contactId;
    private String content;
    private boolean admin;

}