package nm.poolio.utils;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.OverUnder;
import org.springframework.util.CollectionUtils;

import java.util.List;

@RequiredArgsConstructor
public class TicketScorer {
    private final List<NflGame> games;
    int score = 0;
    int fullScore = 0;
    int correct = 0;

    public static @Nullable OverUnder computeOverUnderValue(
            @NotNull Integer gameScore, @NotNull Double bookOverUnderValue) {
        return bookOverUnderValue < gameScore
                ? OverUnder.OVER
                : bookOverUnderValue > gameScore ? OverUnder.UNDER : null;
    }

    private void reset() {
        score = 0;
        fullScore = 0;
        correct = 0;
    }

    public void score(Ticket ticket) {
        reset();

        ticket
                .getSheet()
                .getGamePicks()
                .forEach(
                        (gameKey, teamPicked) -> {
                            var nflGame =
                                    games.stream().filter(g -> g.getId().equals(gameKey)).findAny().orElse(null);
                            if (nflGame != null && teamPicked != null) {
                                var winner = nflGame.findWinner();

                                if (teamPicked.equals(winner)) {
                                    score += 10;
                                    fullScore += 100000;
                                    correct++;
                                }
                            }
                        });

        ticket
                .getSheet()
                .getOverUnderPicks()
                .forEach(
                        (gameKey, playersOverUnderChoice) -> {
                            var nflGame =
                                    games.stream().filter(g -> g.getId().equals(gameKey)).findAny().orElse(null);
                            if (nflGame != null && playersOverUnderChoice != null) {

                                nflGame
                                        .getScore()
                                        .ifPresent(
                                                gameScore -> {
                                                    var bookOverUnderValue = nflGame.getOverUnder();

                                                    if (bookOverUnderValue == null) {
                                                    } else {
                                                        OverUnder overUnderResult =
                                                                computeOverUnderValue(gameScore, bookOverUnderValue);

                                                        if (playersOverUnderChoice == overUnderResult) {
                                                            score += 2;
                                                            fullScore += 20000;
                                                        }
                                                    }
                                                });
                            }
                        });

        ticket
                .getSheet()
                .getSillyPicks()
                .forEach(
                        (sillyKey, sillyAnswer) -> {
                            var nflGame =
                                    games.stream()
                                            .filter(g -> g.getId().equals(sillyAnswer.getGameId()))
                                            .findAny()
                                            .orElse(null);

                            if (nflGame != null && !CollectionUtils.isEmpty(nflGame.getSillyAnswers())) {
                                var sillyCorrectAnswer = nflGame.getSillyAnswers().get(sillyKey);

                                if (sillyCorrectAnswer != null && sillyAnswer.getAnswer() != null) {

                                    if (sillyCorrectAnswer.equals(sillyAnswer.getAnswer())) {
                                        score += 1;
                                        fullScore += 10000;
                                    }
                                }
                            }
                        });

        ticket.setScore(score);
        ticket.setCorrect(correct);

        var optional = games.getLast().getScore();

        Integer diff = null;

        if (optional.isPresent()) {
            diff = optional.get() - ticket.getTieBreaker();
            fullScore -= Math.abs(diff);
        }

        ticket.setScoreString(score + ((diff == null) ? "" : "-" + diff));
        ticket.setFullScore(fullScore);
    }
}
