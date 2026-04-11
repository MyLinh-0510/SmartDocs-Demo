package edu.uni.smartdocs.controllers.user;

import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.models.UserDocumentAction;
import edu.uni.smartdocs.service.DocumentService;
import edu.uni.smartdocs.service.UserDocumentActionService;
import edu.uni.smartdocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserDocumentActionController {

    private final UserDocumentActionService actionService;
    private final UserService userService;
    private final DocumentService documentService;

    // toggle like / save / download
    @PostMapping("/document-action/{docId}/{type}")
    public ResponseEntity<String> toggleAction(
            @PathVariable Long docId,
            @PathVariable UserDocumentAction.ActionType type,
            Principal principal
    ) {
        User user = userService.getCurrentUser(principal);
        boolean added = actionService.toggleAction(user, docId, type);
        return ResponseEntity.ok(added ? "ADDED" : "REMOVED");
    }

    // lấy document theo action
    @GetMapping("/by-action")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public List<DocumentSearchDTO> getDocsByAction(
            Principal principal,
            @RequestParam UserDocumentAction.ActionType type
    ) {
        User user = userService.getCurrentUser(principal);
        return documentService.getDocumentsByAction(user.getId(), type);
    }

    // 🔥 tài liệu liên quan từ lịch sử
    @GetMapping("/related/from-history")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public List<DocumentSearchDTO> getRelatedFromHistory(
            Principal principal
    ) {
        User user = userService.getCurrentUser(principal);

        return actionService
                .getRelatedDocuments(user)
                .stream()
                .map(d -> new DocumentSearchDTO(
                        d.getId(),
                        d.getTitle(),
                        d.getCategory() != null ? d.getCategory().getName() : "",
                        d.getPdfFilename(),
                        0L
                ))
                .toList();
    }

    @PostMapping("/document-viewed/{docId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','CEO')")
    public void documentViewed(
            Principal principal,
            @PathVariable Long docId
    ) {
        User user = userService.getCurrentUser(principal);
        actionService.logViewed(user, docId);
    }


}
