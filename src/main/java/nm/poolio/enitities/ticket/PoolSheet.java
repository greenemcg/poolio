package nm.poolio.enitities.ticket;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import nm.poolio.model.enums.NflTeam;

@Data
public class PoolSheet {
  Map<String, NflTeam> gamePicks = new HashMap<>();

  Integer tieBreaker;
}
