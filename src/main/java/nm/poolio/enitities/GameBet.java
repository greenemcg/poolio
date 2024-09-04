package nm.poolio.enitities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.Season;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "nfl_game_bet")
public class GameBet extends AbstractEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposer_user_id")
    private User proposer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = " acceptor_user_id")
    private User acceptor;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "proposer_transaction_id")
    PoolioTransaction proposerTransaction;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "acceptor_transaction_id")
    PoolioTransaction acceptorTransaction;

    @NotNull
    private String gameId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NflWeek week;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Season season;

    @NotNull
    private Integer spread;

    @NotNull
    private Integer amount;

    private Instant acceptanceDate;
}
