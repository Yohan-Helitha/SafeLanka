package lk.dmc.disaster.reports.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** The photo kept for a report: only its location in storage and its description. */
@Entity
@Table(name = "report_photos")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportPhoto {

  private static final Set<String> MIME_TYPES = Set.of("image/jpeg", "image/png");

  @Id private UUID id;

  @Column(name = "report_id", nullable = false, updatable = false)
  private UUID reportId;

  @Column(name = "file_path", nullable = false, updatable = false)
  private String filePath;

  @Column(name = "mime_type", nullable = false, updatable = false)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false, updatable = false)
  private int sizeBytes;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * @throws BusinessRuleException (422) for a type other than JPEG/PNG or a size outside 1 B-5 MB
   */
  public static ReportPhoto attach(UUID reportId, String filePath, String mimeType, int sizeBytes) {
    if (!MIME_TYPES.contains(mimeType)) {
      throw new BusinessRuleException("The photo must be a JPEG or PNG image.");
    }
    if (sizeBytes <= 0 || sizeBytes > ReportRules.PHOTO_MAX_BYTES) {
      throw new BusinessRuleException("The photo must be between 1 byte and 5 MB.");
    }
    ReportPhoto p = new ReportPhoto();
    p.id = UUID.randomUUID();
    p.reportId = reportId;
    p.filePath = filePath;
    p.mimeType = mimeType;
    p.sizeBytes = sizeBytes;
    return p;
  }
}
