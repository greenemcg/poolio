package nm.poolio.enitities.score;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
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
}
