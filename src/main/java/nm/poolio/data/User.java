package nm.poolio.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import nm.poolio.Theme;
import nm.poolio.enitities.pool.PoolIdName;
import nm.poolio.utils.PhoneNumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "application_user",
    uniqueConstraints = {
      @UniqueConstraint(name = "user_name_uniq", columnNames = "userName"),
      @UniqueConstraint(name = "user_name_uniq", columnNames = "name")
    })
@EntityListeners(AuditingEntityListener.class)
@Setter
@Getter
public class User extends AbstractEntity implements AvatarImageBytes {
  @Setter @Getter @Transient String tempPassword;

  boolean payAsYouGo;

  int creditAmount;
  @Transient List<PoolIdName> poolIdNames;
  @Transient Integer funds;

  @Size(min = 2, max = 255)
  @NotNull
  private String userName;

  @NotNull
  @Size(min = 2, max = 255)
  private String name;

  @Nullable
  @Enumerated(EnumType.STRING)
  private Theme theme;

  @Email private String email;

  @Pattern(regexp = "(^$|[0-9]{10})")
  private String phone;

  @JsonIgnore private String hashedPassword;

  @Enumerated(EnumType.STRING)
  @ElementCollection(fetch = FetchType.EAGER)
  private Set<Role> roles = new HashSet<>(List.of(Role.USER));

  @JsonIgnore
  @Lob
  @JdbcTypeCode(Types.VARBINARY)
  @Column(length = 1000000)
  @Nullable
  private byte[] profilePicture;

  public @Email String getEmail() {
    return email;
  }

  public void setEmail(@Email String email) {
    this.email = email;
  }

  @Transient
  public boolean getAdmin() {
    return this.roles.stream().anyMatch(r -> r.equals(Role.ADMIN));
  }

  @Transient
  public void setAdmin(boolean adminFlag) {
    if (adminFlag) roles.add(Role.ADMIN);
    else roles.remove(Role.ADMIN);
  }

  @JsonIgnore
  @Transient
  public String getPhoneFmt() {
    if (StringUtils.isEmpty(phone)) return "";
    else return PhoneNumberUtils.formatPhoneNumber(phone);
  }
}
