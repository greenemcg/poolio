package nm.poolio.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonbNote {
  String note;
  String user;
  Instant created;
}
