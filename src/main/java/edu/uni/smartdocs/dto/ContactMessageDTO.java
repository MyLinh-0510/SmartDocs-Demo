package edu.uni.smartdocs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageDTO {
    private Long id;
    private boolean edited;
    private Long contactId;
    private String senderEmail;
    private String recipientEmail;
    private String content;
    private String type;

    private String fileUrl;
    private String fileName;

    private List<String> fileUrls;
    private List<String> fileNames;

    private LocalDateTime timestamp;
    private boolean fromAdmin;
    private Boolean deleted;

    private String receiverEmail;

    // Download URLs cho file gốc
    private List<String> downloadUrls;
}