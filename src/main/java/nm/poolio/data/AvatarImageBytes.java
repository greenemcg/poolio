package nm.poolio.data;

import java.io.ByteArrayInputStream;

public interface AvatarImageBytes {
  byte[] getProfilePicture();

  default byte[] getImageResource() {
    if (getProfilePicture() != null && getProfilePicture().length != 0)
      return new ByteArrayInputStream(getProfilePicture()).readAllBytes();
    else return new byte[0];
  }
}
