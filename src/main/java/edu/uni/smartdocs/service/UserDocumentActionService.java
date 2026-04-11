package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.*;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.UserDocumentActionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDocumentActionService {

    private final UserDocumentActionRepository actionRepository;
    private final DocumentRepository documentRepository;
    private final LogDownloadService downloadService;


    // action
    @Transactional
    public boolean toggleAction(
            User user,
            Long docId,
            UserDocumentAction.ActionType type
    ) {

        var existed =
                actionRepository.findByUserIdAndDocumentIdAndActionType(
                        user.getId(), docId, type
                );

        if (existed.isPresent()) {
            actionRepository.delete(existed.get());
            return false;
        }

        Document doc = documentRepository.findById(docId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy document"));

        UserDocumentAction action = new UserDocumentAction();
        action.setUser(user);
        action.setDocument(doc);
        action.setActionType(type);

        actionRepository.save(action);
        return true;
    }

    // Log view
    @Transactional
    public void logViewed(User user, Long docId) {

        Document doc = documentRepository.findById(docId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy document"));

        actionRepository
                .findByUserIdAndDocumentIdAndActionType(
                        user.getId(),
                        docId,
                        UserDocumentAction.ActionType.VIEWED
                )
                .ifPresent(actionRepository::delete);

        UserDocumentAction action = new UserDocumentAction();
        action.setUser(user);
        action.setDocument(doc);
        action.setActionType(UserDocumentAction.ActionType.VIEWED);

        actionRepository.save(action);
    }

    // Tài liệu liên quan
    public List<Document> getRelatedDocuments(User user) {
        return actionRepository
                .findTop5ByUserIdAndActionTypeOrderByCreatedAtDesc(
                        user.getId(),
                        UserDocumentAction.ActionType.VIEWED
                )
                .stream()
                .map(UserDocumentAction::getDocument)
                .toList();
    }



}
