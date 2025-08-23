package nm.poolio.enitities.pool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.AvatarImageBytes;
import nm.poolio.data.User;
import nm.poolio.model.enums.League;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.PoolStatus;
import nm.poolio.model.enums.Season;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.util.Set;

@Data()
@EqualsAndHashCode(callSuper = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "pool",
        uniqueConstraints = {@UniqueConstraint(name = "pool_name_uniq", columnNames = "name")})
public class Pool extends AbstractEntity implements AvatarImageBytes {
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "pool_players",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @OrderBy(value = "name")
    Set<User> players;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "pool_admins",
            joinColumns = @JoinColumn(name = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    Set<User> admins;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer amount;

    @NotNull
    @Min(2)
    @Max(250)
    private Integer maxPlayersPerWeek;

    private boolean includeThursday;

    @NotNull
    @Size(min = 2, max = 50)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    private League league;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Season season;

    @ManyToOne
    @JoinColumn(name = "pay_as_you_go_user_id")
    private User payAsYouGoUser;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "bank_user_id")
    private User bankUser;

    @JsonIgnore
    @Lob
    @JdbcTypeCode(Types.VARBINARY)
    @Column(length = 1000000)
    private byte[] profilePicture;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NflWeek week;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PoolStatus status;

    @JsonIgnore
    @Transient
    public String getPayAsYouGoUserName() {
        return (payAsYouGoUser != null) ? payAsYouGoUser.getName() : "";
    }
}
