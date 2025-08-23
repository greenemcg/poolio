package nm.poolio.enitities.transaction;

import nm.poolio.model.JsonbNote;

import java.time.Instant;

public final class JsonbNoteCreator {
    public static JsonbNote buildJsonbNote(String note, String userName) {
        return JsonbNote.builder().user(userName).created(Instant.now()).note(note).build();
    }
}
