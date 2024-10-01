package nm.poolio.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.ByteArrayInputStream;

public interface AvatarImageBytes {
    @JsonIgnore
    byte[] getProfilePicture();

    @JsonIgnore
    default byte[] getImageResource() {
        if (getProfilePicture() != null && getProfilePicture().length != 0)
            return new ByteArrayInputStream(getProfilePicture()).readAllBytes();
        else return new byte[0];
    }
}
