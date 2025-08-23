package nm.poolio.enitities.bet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.Season;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "nfl_game_bet")
public class GameBet extends AbstractEntity {
  public static final int MIN_AMOUNT = 5;
  public static final int MAX_AMOUNT = 100;
  public static final int MIN_SPREAD = -100;
  public static final int MAX_SPREAD = 100;

  @Transient private NflGame game;
  @Transient private LocalDateTime expiryLocalDateTime;

  @NotNull
  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "proposer_transaction_id")
  PoolioTransaction proposerTransaction;
  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  @JoinTable(
      name = "game_bet_acceptors",
      joinColumns = @JoinColumn(name = "game_bet_id"),
      inverseJoinColumns = @JoinColumn(name = "transaction_id"))
  private Set<PoolioTransaction> acceptorTransactions = new HashSet<>();

  @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  @JoinTable(
      name = "game_bet_winners",
      joinColumns = @JoinColumn(name = "game_bet_id"),
      inverseJoinColumns = @JoinColumn(name = "transaction_id"))
  private Set<PoolioTransaction> resultTransactions = new HashSet<>();

  @NotNull private String gameId;

  @NotNull
  @Enumerated(EnumType.STRING)
  private NflWeek week;

  @NotNull
  @Enumerated(EnumType.STRING)
  private Season season;

  @Min(MIN_SPREAD)
  @Max(MAX_SPREAD)
  @NotNull
  private BigDecimal spread;

  @Transient private Double spreadDouble;

  @Min(MIN_AMOUNT)
  @Max(MAX_AMOUNT)
  @NotNull
  private Integer amount;

  @NotNull private Boolean proposerCanEditTeam;
  @NotNull private Boolean betCanBeSplit;

  @NotNull
  @Enumerated(EnumType.STRING)
  private BetStatus status;

  @NotNull
  @Enumerated(EnumType.STRING)
  private NflTeam teamPicked;

  private Instant acceptanceDate;
  private Instant expiryDate;

  public String getExpirationString() {
    if (expiryDate == null) {
      return "";
    }
    ZoneId zone = ZoneId.of("America/New_York");
    var localDateTime = LocalDateTime.ofInstant(expiryDate, zone);
    return DateTimeFormatter.ofPattern("MMM d, h:mm a").format(localDateTime);
  }

  public void setGame(NflGame game) {
    this.gameId = game.getId();
    this.game = game;
  }

  public User getProposer() {
    return proposerTransaction.getCreditUser();
  }

  public Component getHumanReadableString() {

    VerticalLayout layout = new VerticalLayout();
    layout.add(new Span(createGameDetailsString()));
    layout.add(new Span(createBetDetailsString()));

    LocalDateTime localDateTime =
        LocalDateTime.ofInstant(expiryDate, ZoneId.of("America/New_York"));
    layout.add(
        new Span(
            "Bet expires if no takers: "
                + DateTimeFormatter.ofPattern("E, MMM d, h:mm a").format(localDateTime)));

    return layout;
  }

  public String createGameDetailsString() {
    String str = gameId.split("_")[0].replace("at", " at ");
    str += " (" + spread + ")";
    return "Game: %s %s.".formatted(str, week);
  }

  public String createBetDetailsString() {
    String splits = "";
    if (betCanBeSplit) {
      splits += "- Splits: Yes";
    }

    return "Team picked: %s Amount: $%d %s.".formatted(teamPicked, amount, splits);
  }

  public String getInfoString() {
    return  createBetDetailsString() + " - " + createGameDetailsString();
  }
}
