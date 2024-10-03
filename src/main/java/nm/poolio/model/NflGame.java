package nm.poolio.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.Data;
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
    if (homeScore == null) return null;
    return homeScore.doubleValue();
  }

  @JsonIgnore
  public void setHomeScoreDouble(Double d) {
    if (d == null) homeScore = null;
    else homeScore = d.intValue();
  }

  @JsonIgnore
  public Double getAwayScoreDouble() {
    if (awayScore == null) return null;
    return awayScore.doubleValue();
  }

  @JsonIgnore
  public void setAwayScoreDouble(Double d) {
    if (d == null) awayScore = null;
    else awayScore = d.intValue();
  }

  @JsonIgnore
  public Optional<Integer> getScore() {
    if (homeScore == null && awayScore == null) return Optional.empty();
    return Optional.of(homeScore + awayScore);
  }

  @JsonIgnore
  public LocalDateTime getLocalDateTime() {
    try {
      ZoneId zone = ZoneId.of("America/New_York");
      return LocalDateTime.ofInstant(gameTime, zone);
    } catch (Exception e) {
      return null;
    }
  }

  public String getGameString() {
    return String.format("%s at %s", homeTeam, awayTeam);
  }

  @JsonIgnore
  public NflTeam getWinner() {
    if (awayScore == null || homeScore == null) {
      return NflTeam.TBD;
    }

    if (awayScore > homeScore) {
      return awayTeam;
    }

    if (homeScore > awayScore) {
      return homeTeam;
    }

    return NflTeam.TIE;
  }
}
