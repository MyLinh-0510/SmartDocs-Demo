package edu.uni.smartdocs.service;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class FileConvertService {

    public void convertToPdf(String inputFile, String outputDir) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputDir,
                inputFile
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }

    public void convertToPdfExact(String inputFile, String outputPdfPath) throws Exception {

        File input = new File(inputFile);
        String outputDir = new File(outputPdfPath).getParent();

        // tên LibreOffice sẽ tạo
        String autoPdfName = input.getName().replaceAll("\\.[^.]+$", ".pdf");
        File autoPdf = new File(outputDir, autoPdfName);

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputDir,
                inputFile
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        File target = new File(outputPdfPath);

        // 👉 rename về đúng tên mình muốn
        if (autoPdf.exists() && !autoPdf.getAbsolutePath().equals(target.getAbsolutePath())) {
            autoPdf.renameTo(target);
        }
    }
}
