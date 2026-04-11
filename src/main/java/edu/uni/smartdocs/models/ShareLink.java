package edu.uni.smartdocs.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "sharelink")
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private String ownerEmail;

    private String allowedEmail; // email được mời (PRIVATE)

    @ManyToOne
    private Document document;

    private LocalDateTime expiryTime;

    @Enumerated(EnumType.STRING)
    private ShareType type; // PRIVATE / PUBLIC
}