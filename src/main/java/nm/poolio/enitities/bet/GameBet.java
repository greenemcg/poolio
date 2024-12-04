package nm.poolio.enitities.bet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.PoolioTransaction;
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
  // if a tie we set the two transactions above to amount zero and add note
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
  Set<PoolioTransaction> acceptorTransactions;

  @ManyToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
  @JoinTable(
      name = "game_bet_winners",
      joinColumns = @JoinColumn(name = "game_bet_id"),
      inverseJoinColumns = @JoinColumn(name = "transaction_id"))
  Set<PoolioTransaction> winningTransactions;

  @NotNull private String gameId;

  @NotNull
  @Enumerated(EnumType.STRING)
  private NflWeek week;

  @NotNull
  @Enumerated(EnumType.STRING)
  private Season season;

  @NotNull private Integer spread;
  @NotNull private Integer amount;
  @NotNull private Boolean proposerCanEditTeam;
  @NotNull private Boolean betCanBeSplit;

  @NotNull
  @Enumerated(EnumType.STRING)
  private NflTeam teamPicked;

  private Instant acceptanceDate;
  private Instant expiryDate;

  public User getProposer() {
    return proposerTransaction.getDebitUser();
  }

  public Component getHumanReadableString() {
    var str = gameId; // remove from class

    if (str.indexOf("_") > 0) {
      str = str.substring(0, str.indexOf("_"));
    }

    if (str.indexOf("at") > 0) {
      str = str.replace("at", " at ");
    }

    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.add(new Span("Game: " + str + " " + week));
    verticalLayout.add(
        new Span("Team picked: " + teamPicked + " Spread: " + spread + " Amount: $" + amount));

    ZoneId zone = ZoneId.of("America/New_York");
    var localDateTime = LocalDateTime.ofInstant(expiryDate, zone);

    verticalLayout.add(
        new Span(
            "Bet expires if no takers: "
                + DateTimeFormatter.ofPattern("E, MMM d, h:mm a").format(localDateTime)));

    return verticalLayout;
  }
}
