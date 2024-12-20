package nm.poolio.enitities.transaction;

import nm.poolio.model.JsonbNote;
import nm.poolio.security.AuthenticatedUser;

import java.time.Instant;

public interface NoteCreator {
    AuthenticatedUser getAuthenticatedUser();

    default JsonbNote buildNote(String note) {
        var optional = getAuthenticatedUser().get();

        return JsonbNote.builder()
                .user(optional.isPresent() ? optional.get().getUserName() : "Unknown")
                .created(Instant.now())
                .note(note)
                .build();
    }
}
