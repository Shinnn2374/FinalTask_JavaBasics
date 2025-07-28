package searchengine.model;

import lombok.*;
import javax.persistence.*;

@Entity
@Table(name = "page")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private site site;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @PrePersist
    @PreUpdate
    private void preparePath() {
        if (path != null && !path.startsWith("/")) {
            path = "/" + path;
        }
    }
}