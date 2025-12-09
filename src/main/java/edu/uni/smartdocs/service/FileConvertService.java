package edu.uni.smartdocs.service;

import org.springframework.stereotype.Service;

@Service
public class FileConvertService {

    public void convertToPdf(String inputFile, String outputDir) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",  // đường dẫn soffice
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputDir,
                inputFile
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }
}
