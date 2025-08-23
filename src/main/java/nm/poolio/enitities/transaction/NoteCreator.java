package nm.poolio.enitities.transaction;

import nm.poolio.data.User;
import nm.poolio.model.JsonbNote;
import nm.poolio.security.AuthenticatedUser;

public interface NoteCreator {
    static JsonbNote buildJsonbNote(String note, String userName) {
        return JsonbNoteCreator.buildJsonbNote(note, userName);
    }

    AuthenticatedUser getAuthenticatedUser();

    default JsonbNote buildNote(String note) {
        return buildJsonbNote(
                note, getAuthenticatedUser().get().map(User::getUserName).orElse("System"));
    }
}
