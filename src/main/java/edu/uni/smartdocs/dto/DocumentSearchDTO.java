package edu.uni.smartdocs.dto;

import edu.uni.smartdocs.models.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchDTO {

    private Long id;
    private String title;
    private String category;
    private String pdfFilename;
    private Long downloadCount;
    private String meta;

    // icon state
    private boolean favorite;
    private boolean saved;
    private boolean pinned;

    // constructor dùng phổ biến
    public DocumentSearchDTO(
            Long id,
            String title,
            String category,
            String pdfFilename,
            String meta
    ) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.pdfFilename = pdfFilename;
        this.meta = meta;
    }

    // constructor có icon (KHÔNG count)
    public DocumentSearchDTO(
            Document d,
            boolean favorite,
            boolean saved,
            boolean pinned
    ) {
        this(d, favorite, saved, pinned, 0L);
    }

    // constructor đầy đủ icon + download
    public DocumentSearchDTO(
            Document d,
            boolean favorite,
            boolean saved,
            boolean pinned,
            long downloadCount
    ) {
        this.id = d.getId();
        this.title = d.getTitle();
        this.category =
                d.getCategory() != null ? d.getCategory().getName() : "";
        this.pdfFilename = d.getPdfFilename();
        this.meta = d.getMeta(); // 🔥 THÊM DÒNG NÀY
        this.favorite = favorite;
        this.saved = saved;
        this.pinned = pinned;
        this.downloadCount = downloadCount;
    }

    // 🔥 constructor cho JPQL popular
    public DocumentSearchDTO(
            Long id,
            String title,
            String category,
            String pdfFilename,
            Long downloadCount
    ) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.pdfFilename = pdfFilename;
        this.downloadCount = downloadCount;
    }

}
