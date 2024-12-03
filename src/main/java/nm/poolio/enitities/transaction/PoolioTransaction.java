package nm.poolio.enitities.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.model.JsonbNote;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "poolio_transaction")
public class PoolioTransaction extends AbstractEntity {
    @Transient
    Integer temporalAmount;
    @Transient
    Integer sequence;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "debit_user_id")
    User debitUser; // cash is incoming

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_user_id")
    User creditUser; // cash is outgoing

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pay_as_u_go_user_id")
    User payAsYouGoUser; // Memo user when player plays under another user's wallet

    @NotNull
    @Min(1)
    @Max(1000)
    Integer amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    PoolioTransactionType type;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notes", columnDefinition = "jsonb")
    private List<JsonbNote> notes;

    @JsonIgnore
    @Transient
    public String getNote() {
        return (CollectionUtils.isEmpty(notes)) ? null : notes.getFirst().getNote();
    }
}
