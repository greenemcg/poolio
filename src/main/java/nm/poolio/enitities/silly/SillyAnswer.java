package nm.poolio.enitities.silly;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SillyAnswer {
    String answer;
    String gameId;
}
