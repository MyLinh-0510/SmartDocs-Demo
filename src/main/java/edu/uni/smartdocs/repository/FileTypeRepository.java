package edu.uni.smartdocs.repository;

import edu.uni.smartdocs.models.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileTypeRepository extends JpaRepository<FileType, Long> {
}

