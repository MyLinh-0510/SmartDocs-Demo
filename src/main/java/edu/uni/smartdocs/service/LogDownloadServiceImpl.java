package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.Document;
import edu.uni.smartdocs.dto.DocumentSearchDTO;
import edu.uni.smartdocs.models.LogDownload;
import edu.uni.smartdocs.models.User;
import edu.uni.smartdocs.repository.DocumentRepository;
import edu.uni.smartdocs.repository.LogDownloadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogDownloadServiceImpl implements LogDownloadService {

    private final LogDownloadRepository logDownloadRepository;
    private final DocumentRepository documentRepository;

    @Override
    public void logDownload(User user, Long docId) {

        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        LogDownload log = new LogDownload();
        log.setUser(user);
        log.setDocument(document);

        logDownloadRepository.save(log);
    }

    @Override
    public long getDownloadCountByDoc(Long docId) {
        return logDownloadRepository.countByDocumentId(docId);
    }

    @Override
    public long getTotalDownloads() {
        return logDownloadRepository.countAllDownloads();
    }

    @Override
    public List<DocumentSearchDTO> getTop5Downloaded() {

        return logDownloadRepository.findTopDownloaded()
                .stream()
                .limit(5)
                .map(obj -> {
                    Long docId = (Long) obj[0];
                    Long count = (Long) obj[1];

                    Document doc = documentRepository.findById(docId).orElse(null);
                    if (doc == null) return null;

                    DocumentSearchDTO dto = new DocumentSearchDTO(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getCategory() != null ? doc.getCategory().getName() : "",
                            doc.getPdfFilename(),
                            doc.getMeta()
                    );

                    dto.setDownloadCount(count);
                    return dto;
                })
                .filter(d -> d != null)
                .toList();
    }
}

