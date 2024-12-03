package nm.poolio.enitities.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.Season;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "ticket",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "ticket_uniq",
          columnNames = {"user_id", "pool_id", "week", "season"})
    })
public class Ticket extends AbstractEntity {
  @Transient int score;
  @Transient String scoreString;
  @Transient int fullScore; // using higher values to allow for tiebreaker
  @Transient String rankString;
  @Transient Integer rank;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "transaction_id")
  PoolioTransaction transaction;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "winning_transaction_id")
  PoolioTransaction winningTransaction;

  @NotNull
  @Enumerated(EnumType.STRING)
  private NflWeek week;

  @NotNull
  @Enumerated(EnumType.STRING)
  private Season season;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  private User player;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "pool_id")
  private Pool pool;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "sheet", columnDefinition = "jsonb")
  private PoolSheet sheet;

  @JsonIgnore
  @Transient
  public Integer getTieBreaker() {
    return sheet != null ? sheet.getTieBreaker() : null;
  }

  @JsonIgnore
  @Transient
  public String getPicksString() {
    return sheet != null ? createPicksString(sheet.getGamePicks()) : null;
  }

  private String createPicksString(Map<String, NflTeam> gamePicks) {
    var names = gamePicks.values().stream().map(t -> t == null ? "" : t.name()).toList();

    return String.join(",", names);
  }

  @Transient
  public String findPlayerName() {
    return player != null ? player.getName() : "";
  }
}
