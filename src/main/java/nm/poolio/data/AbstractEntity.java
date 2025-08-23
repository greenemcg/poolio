package nm.poolio.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@MappedSuperclass
@Getter
@Setter
public abstract class AbstractEntity {
  @JsonIgnore TimeZone timeZone;

  // @Nullable
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgenerator")
  @SequenceGenerator(name = "idgenerator", initialValue = 10)
  private Long id;

  @Version private int version;

  @Column(name = "created_date", nullable = false, updatable = false)
  @CreatedDate
  private Instant createdDate;

  @Column(name = "modified_date")
  @LastModifiedDate
  private Instant modifiedDate;

  @Column(name = "created_by")
  @CreatedBy
  private String createdBy;

  @Column(name = "modified_by")
  @LastModifiedBy
  private String modifiedBy;

  @Nullable
  @Column(name = "inactive_date")
  private Instant inactiveDate;

  @Override
  public int hashCode() {
    if (id != null) {
      return id.hashCode();
    }
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AbstractEntity that)) {
      return false; // null or not an AbstractEntity class
    }
    if (id != null) {
      return id.equals(that.getId());
    }
    return super.equals(that);
  }

  @JsonIgnore
  public @Nullable LocalDateTime getCreatedLocalDateTime() {
    try {
      ZoneId zone = ZoneId.of("America/New_York");
      return LocalDateTime.ofInstant(createdDate, zone);
    } catch (Exception e) {
      return null;
    }
  }

  @JsonIgnore
  public LocalDateTime getCreatedLocalDateTimeWithZone() {
    if (timeZone == null) return getCreatedLocalDateTime();

    try {
      return LocalDateTime.ofInstant(createdDate, timeZone.toZoneId());
    } catch (Exception e) {
      return null;
    }
  }

  @JsonIgnore
  public Instant getModifiedSortTime() {
    return (modifiedDate != null) ? modifiedDate : createdDate;
  }
}
