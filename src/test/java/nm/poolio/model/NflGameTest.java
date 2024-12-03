package nm.poolio.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import nm.poolio.model.enums.NflTeam;
import org.junit.jupiter.api.Test;

class NflGameTest {
  @Test
  void getHomeScoreDouble_returnsCorrectValue() {
    NflGame game = new NflGame();
    game.setHomeScore(21);
    assertEquals(21.0, game.getHomeScoreDouble());
  }

  @Test
  void getHomeScoreDouble_returnsNullWhenHomeScoreIsNull() {
    NflGame game = new NflGame();
    game.setHomeScore(null);
    assertNull(game.getHomeScoreDouble());
  }

  @Test
  void setHomeScoreDouble_setsCorrectValue() {
    NflGame game = new NflGame();
    game.setHomeScoreDouble(21.0);
    assertEquals(21, game.getHomeScore());
  }

  @Test
  void setHomeScoreDouble_setsNullWhenInputIsNull() {
    NflGame game = new NflGame();
    game.setHomeScoreDouble(null);
    assertNull(game.getHomeScore());
  }

  @Test
  void getAwayScoreDouble_returnsCorrectValue() {
    NflGame game = new NflGame();
    game.setAwayScore(14);
    assertEquals(14.0, game.getAwayScoreDouble());
  }

  @Test
  void getAwayScoreDouble_returnsNullWhenAwayScoreIsNull() {
    NflGame game = new NflGame();
    game.setAwayScore(null);
    assertNull(game.getAwayScoreDouble());
  }

  @Test
  void setAwayScoreDouble_setsCorrectValue() {
    NflGame game = new NflGame();
    game.setAwayScoreDouble(14.0);
    assertEquals(14, game.getAwayScore());
  }

  @Test
  void setAwayScoreDouble_setsNullWhenInputIsNull() {
    NflGame game = new NflGame();
    game.setAwayScoreDouble(null);
    assertNull(game.getAwayScore());
  }

  @Test
  void getScore_returnsCorrectSum() {
    NflGame game = new NflGame();
    game.setHomeScore(21);
    game.setAwayScore(14);
    assertEquals(Optional.of(35), game.getScore());
  }

  @Test
  void getScore_returnsEmptyWhenScoresAreNull() {
    NflGame game = new NflGame();
    game.setHomeScore(null);
    game.setAwayScore(null);
    assertEquals(Optional.empty(), game.getScore());
  }

  @Test
  void getLocalDateTime_returnsCorrectLocalDateTime() {
    NflGame game = new NflGame();
    Instant instant = Instant.parse("2023-10-01T12:00:00.00Z");
    game.setGameTime(instant);
    LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.of("America/New_York"));
    assertEquals(expected, game.getLocalDateTime());
  }

  @Test
  void getLocalDateTime_returnsNullOnException() {
    NflGame game = new NflGame();
    game.setGameTime(null);
    assertNull(game.getLocalDateTime());
  }

  @Test
  void getGameString_returnsCorrectString() {
    NflGame game = new NflGame();
    game.setHomeTeam(NflTeam.NE);
    game.setAwayTeam(NflTeam.NYG);
    assertEquals("NYG at NE", game.getGameString());
  }

  @Test
  void findWinner_returnsCorrectWinner() {
    NflGame game = new NflGame();
    game.setHomeTeam(NflTeam.NE);
    game.setHomeScore(21);
    game.setAwayScore(14);
    assertEquals(NflTeam.NE, game.findWinner());
  }

  @Test
  void findWinner_returnsTieWhenScoresAreEqual() {
    NflGame game = new NflGame();
    game.setHomeScore(21);
    game.setAwayScore(21);
    assertEquals(NflTeam.TIE, game.findWinner());
  }

  @Test
  void findWinner_returnsTBDWhenScoresAreNull() {
    NflGame game = new NflGame();
    game.setHomeScore(null);
    game.setAwayScore(null);
    assertEquals(NflTeam.TBD, game.findWinner());
  }

  @Test
  void findWinnerSpread_returnsAwayTeamWhenAwayScoreIsGreaterThanHomeScorePlusSpread() {
    NflGame game = new NflGame();
    game.setHomeScore(14);
    game.setAwayScore(21);
    game.setAwayTeam(NflTeam.GB);
    assertEquals(NflTeam.GB, game.findWinnerSpread(BigDecimal.valueOf(6)));
  }

  @Test
  void findWinnerSpread_returnsHomeTeamWhenSpreadIsGreaterThanAwayScore() {
    NflGame game = new NflGame();
    game.setHomeScore(14);
    game.setHomeTeam(NflTeam.NE);
    game.setAwayScore(10);
    assertEquals(NflTeam.NE, game.findWinnerSpread(BigDecimal.valueOf(15)));
  }

  @Test
  void findWinnerSpread_AwayTeamFavoredNegativeSpreadAndAwayWins() {
    NflGame game = new NflGame();
    game.setHomeScore(20);
    game.setAwayScore(20);
    game.setAwayTeam(NflTeam.NE);
    assertEquals(NflTeam.NE, game.findWinnerSpread(BigDecimal.valueOf(-1)));
  }

  @Test
  void findWinnerSpread_AwayTeamFavoredNegativeSpreadAndHomeWins() {
    NflGame game = new NflGame();
    game.setHomeScore(25);
    game.setHomeTeam(NflTeam.NE);
    game.setAwayScore(20);
    assertEquals(NflTeam.NE, game.findWinnerSpread(BigDecimal.valueOf(-3.5)));
  }

  @Test
  void findWinnerSpread_HomeWinsWithHook() {
    NflGame game = new NflGame();
    game.setHomeScore(25);
    game.setHomeTeam(NflTeam.NE);
    game.setAwayScore(30);
    assertEquals(NflTeam.NE, game.findWinnerSpread(BigDecimal.valueOf(5.5)));
  }

  @Test
  void findWinnerSpread_AwayWinsWithHook() {
    NflGame game = new NflGame();
    game.setHomeScore(10);
    game.setAwayScore(5);
    game.setAwayTeam(NflTeam.NE);
    assertEquals(NflTeam.NE, game.findWinnerSpread(BigDecimal.valueOf(-5.5)));
  }

  @Test
  void findWinnerSpread_TieWithZeroSpread() {
    NflGame game = new NflGame();
    game.setHomeScore(10);
    game.setAwayScore(10);
    assertEquals(NflTeam.TIE, game.findWinnerSpread(BigDecimal.valueOf(0)));
  }

  @Test
  void findWinnerSpread_returnsTieWhenScoresAreEqualWithSpread() {
    NflGame game = new NflGame();
    game.setHomeScore(14);
    game.setAwayScore(14);
    assertEquals(NflTeam.TIE, game.findWinnerSpread(BigDecimal.valueOf(0)));
  }

  @Test
  void findWinnerSpread_returnsTBDWhenScoresAreNull() {
    NflGame game = new NflGame();
    game.setHomeScore(null);
    game.setAwayScore(null);
    assertEquals(NflTeam.TBD, game.findWinnerSpread(BigDecimal.valueOf(7)));
  }

  @Test
  void findWinnerSpread_returnsTBDWhenHomeScoreIsNull() {
    NflGame game = new NflGame();
    game.setHomeScore(null);
    game.setAwayScore(14);
    assertEquals(NflTeam.TBD, game.findWinnerSpread(BigDecimal.valueOf(7)));
  }

  @Test
  void findWinnerSpread_returnsTBDWhenAwayScoreIsNull() {
    NflGame game = new NflGame();
    game.setHomeScore(14);
    game.setAwayScore(null);
    assertEquals(NflTeam.TBD, game.findWinnerSpread(BigDecimal.valueOf(7)));
  }

}
