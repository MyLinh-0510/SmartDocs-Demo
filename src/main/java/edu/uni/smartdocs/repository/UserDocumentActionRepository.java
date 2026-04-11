package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.models.UserDocumentAction;
import edu.uni.smartdocs.models.UserDocumentAction.ActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDocumentActionRepository
        extends JpaRepository<UserDocumentAction, Long> {

    // Kiểm tra hành động đã tồn tại hay chưa
    // VD đã ghim tài liệu hay chưa
    boolean existsByUserIdAndDocumentIdAndActionType(
            Long userId,
            Long documentId,
            ActionType actionType
    );

    // Lấy đúng 1 hành động cụ thể của user
    // Click 2 lần thì tìm action delete
    Optional<UserDocumentAction>
    findByUserIdAndDocumentIdAndActionType(
            Long userId,
            Long documentId,
            ActionType actionType
    );

    // Lấy danh sách hành động theo loại yêu thích/lưu/ghim
    // Load lại danh sách gắn trực tiếp với /user/saved-api?type=FAVORITE
    List<UserDocumentAction>
    findByUserIdAndActionType(
            Long userId,
            ActionType actionType
    );

    // Lấy 5 hành động gần nhất theo category
    // Tài liệu liên quan và tài liệu vửa tải
    List<UserDocumentAction>
    findTop5ByUserIdAndActionTypeOrderByCreatedAtDesc(
            Long userId,
            ActionType actionType
    );

    // Lấy hành động gần nhát theo 1 loại duy nhất
    Optional<UserDocumentAction>
    findTopByUserIdAndActionTypeOrderByCreatedAtDesc(
            Long userId,
            ActionType actionType
    );

    // Lấy hành động gần nhất của user bất kì catagory nào
    Optional<UserDocumentAction> findTopByUserOrderByCreatedAtDesc(User user);


}
