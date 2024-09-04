package nm.poolio.enitities.silly;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "silly_picks",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "silly_uniq",
          columnNames = {"game_id"})
    })
public class SillyPicks extends AbstractEntity {
  private String gameId;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "sillies", columnDefinition = "jsonb")
  private List<SillyQuestion> sillies;
}
