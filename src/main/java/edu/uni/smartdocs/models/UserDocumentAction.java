package edu.uni.smartdocs.models;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "user_document_actions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "document_id", "action_type"})
        }
)
public class UserDocumentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;

    private String tagValue;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ActionType {
        FAVORITE,
        SAVED,
        TAG,
        VIEWED,
        PINNED
    }
}
