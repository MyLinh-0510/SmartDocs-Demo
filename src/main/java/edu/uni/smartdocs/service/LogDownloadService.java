package edu.uni.smartdocs.service;

import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.User;

import java.util.List;

public interface LogDownloadService {

    void logDownload(User user, Long docId);

    long getDownloadCountByDoc(Long docId);

    long getTotalDownloads();

    List<DocumentSearchDTO> getTop5Downloaded();

}
