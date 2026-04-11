package edu.uni.smartdocs.models;

import lombok.Data;

@Data
public class DocumentActionRequest {
    private Long documentId;
    private UserDocumentAction.ActionType actionType;
}

