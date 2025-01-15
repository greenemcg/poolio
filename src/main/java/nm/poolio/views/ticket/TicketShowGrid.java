package nm.poolio.views.ticket;

import static nm.poolio.utils.TicketScorer.computeOverUnderValue;
import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.dom.Style;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javax.annotation.Nullable;
import nm.poolio.enitities.score.GameScore;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.OverUnder;
import nm.poolio.vaadin.PoolioGrid;
import org.springframework.util.CollectionUtils;

public interface TicketShowGrid extends PoolioGrid<NflGame> {
  Grid<NflGame> getTicketGrid();

  @Override
  default Grid<NflGame> getGrid() {
    return getTicketGrid();
  }

  private Icon createIcon(VaadinIcon vaadinIcon) {
    Icon icon = vaadinIcon.create();
    icon.getStyle().set("padding", "var(--lumo-space-xs)");
    return icon;
  }

  private Component createWinnerComponent(Boolean pickCorrect) {
    if (pickCorrect == null) return new Span("");
    else if (pickCorrect) return createIcon(VaadinIcon.PLUS);
    else return createIcon(VaadinIcon.MINUS);
  }

  private void createWinnerText(Boolean pickCorrect, Style style) {
    if (pickCorrect == null) style.set("font-style", "oblique");
    else if (!pickCorrect) style.set("text-decoration", "line-through");
  }

  private Component createStylesTeamComponent(
      NflGame nflGame,
      @Nullable NflTeam teamToCheck,
      Map<String, NflTeam> gamePicks,
      Map<String, GameScore> scores) {

    var pickedTeam = gamePicks.get(nflGame.getId());

    if (teamToCheck == null) {
      return new Span("");
    }

    if (pickedTeam != null) {
      var gameScore = scores.get(nflGame.getId());
      Boolean pickedCorrectly = null;

      if (gameScore != null) {
        nflGame.setHomeScore(gameScore.getHomeScore());
        nflGame.setAwayScore(gameScore.getAwayScore());

        var winner = nflGame.findWinner();
        pickedCorrectly = winner == teamToCheck;
      }

      if (pickedTeam == teamToCheck) {
        Span confirmed2 =
            new Span(new Span(teamToCheck.name()), createWinnerComponent(pickedCorrectly));

        if (Boolean.FALSE.equals(pickedCorrectly))
          confirmed2.getElement().getStyle().set("color", "red");
        else confirmed2.getElement().getStyle().set("font-weight", "bold");

        createWinnerText(pickedCorrectly, confirmed2.getElement().getStyle());
        return confirmed2;
      } else {
        Span confirmed2 = new Span(new Span(teamToCheck.name()));
        createWinnerText(pickedCorrectly, confirmed2.getElement().getStyle());

        return confirmed2;
      }
    }

    return new Span(teamToCheck.name());
  }

  default void decorateTicketGrid(Ticket ticket, Map<String, GameScore> scores) {
    Map<String, NflTeam> gamePicks = ticket.getSheet().getGamePicks();
    Map<String, OverUnder> overUnderPicks = ticket.getSheet().getOverUnderPicks();

    getGrid()
        .addColumn(
            new ComponentRenderer<>(
                game -> createStylesTeamComponent(game, game.getAwayTeam(), gamePicks, scores)))
        .setHeader(createIconSpan(AWAY_ICON, "Away"))
        .setWidth("86px")
        .setFlexGrow(0)
        .setTextAlign(ColumnTextAlign.CENTER);

    if (!CollectionUtils.isEmpty(scores))
      createColumn(NflGame::getAwayScore, createIconSpan(AWAY_ICON, "Score"))
          .setWidth("50px")
          .setFlexGrow(0);

    getGrid()
        .addColumn(
            new ComponentRenderer<>(
                game -> createStylesTeamComponent(game, game.getHomeTeam(), gamePicks, scores)))
        .setHeader(createIconSpan(HOME_ICON, "Home"))
        .setWidth("86px")
        .setFlexGrow(0)
        .setTextAlign(ColumnTextAlign.CENTER);

    if (!CollectionUtils.isEmpty(scores))
      createColumn(NflGame::getHomeScore, createIconSpan(HOME_ICON, "Score"))
          .setWidth("50px")
          .setFlexGrow(0);

    if (!CollectionUtils.isEmpty(overUnderPicks))
      getGrid()
          .addColumn(
              new ComponentRenderer<>(
                  game -> {
                    var overUnder = overUnderPicks.get(game.getId());
                    if (overUnder == null) return new Span("");
                    else {
                      var span = new Span(overUnder.name() + " (" + game.getOverUnder() + ")");

                      if (game.getScore().isPresent()) {
                        var gameScore = game.getScore().get();
                        var overUnderResult = computeOverUnderValue(gameScore, game.getOverUnder());
                        if (overUnderResult == overUnder) {
                          span.getStyle().set("font-weight", "bold");
                        } else {
                          span.getStyle().set("text-decoration", "line-through");
                        }
                      }

                      return span;
                    }
                  }))
          .setHeader(createIconSpan(OVER_UNDER_ICON, "Over/Under"))
          .setWidth("150px")
          .setFlexGrow(0)
          .setTextAlign(ColumnTextAlign.CENTER);

    // This date formatter will show the day of week
    getGrid()
        .addColumn(
            new LocalDateTimeRenderer<>(
                NflGame::getLocalDateTime, () -> DateTimeFormatter.ofPattern("E, MMM d, h:mm a")))
        .setHeader(createIconSpan(GAME_TIME_ICON, "Game Time (EST)"))
        .setAutoWidth(true);
  }
}
