package nm.poolio.enitities.ticket;

import lombok.Data;
import nm.poolio.model.enums.NflTeam;

import java.util.HashMap;
import java.util.Map;

@Data
public class PoolSheet {
    Map<String, NflTeam> gamePicks = new HashMap<>();

    Integer tieBreaker;
}
