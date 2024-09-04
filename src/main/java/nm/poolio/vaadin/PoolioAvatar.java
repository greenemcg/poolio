package nm.poolio.vaadin;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import nm.poolio.data.AvatarImageBytes;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.model.enums.NflTeam;

public interface PoolioAvatar {
  default Avatar createNflTeamAvatar(NflTeam nflTeam, AvatarVariant variant) {
    Avatar avatar = new Avatar(nflTeam.name());
     avatar.setImage("/icons/nfl/" + nflTeam.name() + ".svg");
   // avatar.setImage("/icons/ncaa/" + nflTeam.name() + ".png");

    avatar.addThemeVariants(variant);
    return avatar;
  }

  default Avatar createPoolAvatar(Pool pool, AvatarVariant variant) {
    return decorateAvatar(pool, new Avatar(pool.getName()), variant);
  }

  default Avatar createUserAvatar(User user, AvatarVariant variant) {
    var avatar = new Avatar(user.getName());
    avatar.setColorIndex((int) (user.getId() % 8));

    return decorateAvatar(user, avatar, variant);
  }

  private Avatar decorateAvatar(AvatarImageBytes imageBytes, Avatar avatar, AvatarVariant variant) {
    avatar.setImageResource(
        new StreamResource(
            "profile-pic", () -> new ByteArrayInputStream(imageBytes.getImageResource())));
    avatar.addThemeVariants(variant);
    avatar.getElement().setAttribute("tooltip", "My tooltip");
    avatar.setTooltipEnabled(true);

    return avatar;
  }
}
