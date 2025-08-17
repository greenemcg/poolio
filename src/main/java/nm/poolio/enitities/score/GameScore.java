package nm.poolio.enitities.score;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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
    name = "game_score",
    uniqueConstraints = {@UniqueConstraint(name = "game_score_uniq", columnNames = "game_id")})
public class GameScore extends AbstractEntity {
  @Min(0)
  @Max(100)
  private Integer homeScore;

  @Min(0)
  @Max(100)
  private Integer awayScore;

  @NotNull private String gameId;

  @Column(name = "spread", precision = 3, scale = 1)
  private BigDecimal spread;

  @Column(name = "overUnder", precision = 4, scale = 1)
  private BigDecimal overUnder;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "sillyAnswers", columnDefinition = "jsonb")
  private Map<String, String> sillyAnswers = new HashMap<>();
}
