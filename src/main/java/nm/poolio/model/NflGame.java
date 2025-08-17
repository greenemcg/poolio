package nm.poolio.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import nm.poolio.enitities.silly.SillyPicks;
import nm.poolio.enitities.silly.SillyQuestion;
import nm.poolio.model.enums.League;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.Season;

@Data
public class NflGame {
  private static final String PATTERN_FORMAT = "dd.MM.yyyy";

  NflTeam homeTeam;
  NflTeam awayTeam;
  Season season;
  String id;
  Integer week;

  @Nullable Double overUnder;
  @Nullable Double spread;

  @Nullable List<SillyQuestion> sillies;
  @Nullable @JsonIgnore Map<String, String> sillyAnswers = new HashMap<>();

  @JsonIgnore Integer awayScore;
  @JsonIgnore Integer homeScore;

  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
      timezone = "America/New_York")
  Instant gameTime;

  public static League getLeague() {
    return League.NFL;
  }

  @JsonIgnore
  public Double getHomeScoreDouble() {
    return homeScore == null ? null : homeScore.doubleValue();
  }

  @JsonIgnore
  public void setHomeScoreDouble(Double d) {
    homeScore = d == null ? null : d.intValue();
  }

  @JsonIgnore
  public Double getAwayScoreDouble() {
    return awayScore == null ? null : awayScore.doubleValue();
  }

  @JsonIgnore
  public void setAwayScoreDouble(Double d) {
    awayScore = d == null ? null : d.intValue();
  }

  @JsonIgnore
  public Optional<Integer> getScore() {
    return Optional.ofNullable(homeScore)
        .flatMap(h -> Optional.ofNullable(awayScore).map(a -> h + a));
  }

  @JsonIgnore
  public LocalDateTime getLocalDateTime() {
    try {
      return LocalDateTime.ofInstant(gameTime, ZoneId.of("America/New_York"));
    } catch (Exception e) {
      return null;
    }
  }

  public String getGameString() {
    return String.format("%s at %s", awayTeam, homeTeam);
  }

  @JsonIgnore
  public @NotNull NflTeam findWinner() {
    if (awayScore == null || homeScore == null) return NflTeam.TBD;
    if (awayScore > homeScore) return awayTeam;
    if (homeScore > awayScore) return homeTeam;
    return NflTeam.TIE;
  }

  public @NotNull NflTeam findWinnerSpread(@NotNull BigDecimal spread) {
    if (awayScore == null || homeScore == null) return NflTeam.TBD;

    if (awayScore > homeScore + spread.doubleValue()) return awayTeam;

    if (homeScore + spread.doubleValue() > awayScore) return homeTeam;

    return NflTeam.TIE;
  }
}
