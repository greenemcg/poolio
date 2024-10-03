package nm.poolio.enitities.bet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.model.enums.BetStatus;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.Season;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "nfl_game_bet")
public class GameBet extends AbstractEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "proposer_transaction_id")
    private PoolioTransaction proposerTransaction;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "game_bet_acceptors",
            joinColumns = @JoinColumn(name = "game_bet_id"),
            inverseJoinColumns = @JoinColumn(name = "transaction_id"))
    private Set<PoolioTransaction> acceptorTransactions = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "game_bet_winners",
            joinColumns = @JoinColumn(name = "game_bet_id"),
            inverseJoinColumns = @JoinColumn(name = "transaction_id"))
    private Set<PoolioTransaction> winningTransactions;

    @NotNull
    private String gameId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NflWeek week;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Season season;

    @NotNull
    private BigDecimal spread;

    @NotNull
    private Integer amount;

    @NotNull
    private Boolean proposerCanEditTeam;

    @NotNull
    private Boolean betCanBeSplit;

    @NotNull
    @Enumerated(EnumType.STRING)
    private BetStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NflTeam teamPicked;

    private Instant acceptanceDate;
    private Instant expiryDate;

    public User getProposer() {
        return proposerTransaction.getCreditUser();
    }

    public Component getHumanReadableString() {
        String str = gameId.split("_")[0].replace("at", " at ");
        VerticalLayout layout = new VerticalLayout();
        layout.add(new Span("Game: " + str + " " + week));
        layout.add(new Span("Team picked: " + teamPicked + " Spread: " + spread + " Amount: $" + amount));

        ZoneId zone = ZoneId.of("America/New_York");
        LocalDateTime localDateTime = LocalDateTime.ofInstant(expiryDate, zone);
        layout.add(new Span("Bet expires if no takers: " + DateTimeFormatter.ofPattern("E, MMM d, h:mm a").format(localDateTime)));

        return layout;
    }
}
