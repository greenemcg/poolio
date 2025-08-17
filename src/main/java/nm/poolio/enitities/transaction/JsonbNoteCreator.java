package nm.poolio.enitities.transaction;

import java.time.Instant;
import nm.poolio.data.User;
import nm.poolio.model.JsonbNote;
import nm.poolio.security.AuthenticatedUser;

public final class JsonbNoteCreator {
 public static JsonbNote buildJsonbNote(String note, String userName) {
    return JsonbNote.builder().user(userName).created(Instant.now()).note(note).build();
  }
}
