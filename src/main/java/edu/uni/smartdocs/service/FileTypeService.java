package edu.uni.smartdocs.service;

import edu.uni.smartdocs.models.FileType;
import edu.uni.smartdocs.repository.FileTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileTypeService {

    private final FileTypeRepository fileTypeRepository;

    public List<FileType> findAll() {
        return fileTypeRepository.findAll();
    }

    public FileType findById(Long id) {
        return fileTypeRepository.findById(id).orElse(null);
    }

    public FileType save(FileType fileType) {
        return fileTypeRepository.save(fileType);
    }

    public void deleteById(Long id) {
        fileTypeRepository.deleteById(id);
    }
}