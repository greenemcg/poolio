package nm.poolio.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class JsonbNote {
    String note;
    String user;
    Instant created;
}
