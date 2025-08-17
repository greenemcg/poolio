package nm.poolio.enitities.ticket;

import lombok.Data;
import nm.poolio.enitities.silly.SillyAnswer;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.OverUnder;

import java.util.HashMap;
import java.util.Map;

@Data
public class PoolSheet {
    Map<String, NflTeam> gamePicks = new HashMap<>();

    Map<String, OverUnder> overUnderPicks = new HashMap<>();

    Integer tieBreaker;

    Map<String, SillyAnswer> sillyPicks = new HashMap<>();
}


