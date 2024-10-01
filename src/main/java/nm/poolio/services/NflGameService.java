package nm.poolio.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.score.GameScoreService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflWeek;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NflGameService implements Serializable {
    @Serial
    private static final long serialVersionUID = -3179691057002713470L;
    private final GameScoreService gameScoreService;
    List<NflGame> gameList = new ArrayList<>();
    Map<String, NflGame> gameMap = new HashMap<>();

    @SneakyThrows
    @PostConstruct
    public void init() {
        InputStream inputStream = getClass().getResourceAsStream("/nfl/nfl_season_2024.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        NflGame[] gamesArray = mapper.readValue(inputStream, NflGame[].class);

        gameList = Arrays.asList(gamesArray);
        gameList.sort(Comparator.comparing(NflGame::getGameTime));
        gameList = Collections.unmodifiableList(gameList);

        gameMap =
                Collections.unmodifiableMap(
                        gameList.stream().collect(Collectors.toMap(NflGame::getId, Function.identity())));

        //    gameList.stream()
        //        .forEach(
        //            g -> {
        //              var day = DateTimeFormatter.ofPattern("E").format(g.getLocalDateTime());
        //
        //              g.setId(
        //                  g.getHomeTeam()
        //                      + "at"
        //                      + g.getAwayTeam()
        //                      + "_W"
        //                      + g.getWeek()
        //                      + "_"
        //                      + day
        //                      + "_"
        //                      + g.getId());
        //            });
        //
        //    ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        //    writer.writeValue(new File("/tmp/nfl_season_2024.json"), gameList);
    }

    public NflGame findGameById(String id) {
        var game = gameMap.get(id);
        if (game == null) {
            throw new IllegalArgumentException("Game not found with id " + id);
        }

        addScores(game);

        return game;
    }

    public List<NflGame> getGamesForPool(Pool pool, NflWeek week) {
        return getWeeklyGamesThursdayFiltered(week, pool.isIncludeThursday());
    }

    public List<NflGame> getWeeklyGamesForPool(Pool pool) {
        return getGamesForPool(pool, pool.getWeek());
    }

    public List<NflGame> getWeeklyGamesForPool(Pool pool, NflWeek week) {
        return getGamesForPool(pool, week);
    }

    public List<NflGame> getWeeklyGamesThursdayFiltered(NflWeek week, boolean includeThurs) {
        List<NflGame> weekGames = getWeeklyGames(week);

        var filtered = filterByThurs(includeThurs, weekGames);

        filtered.parallelStream().forEach(this::addScores);

        return filtered;
    }

    void addScores(NflGame g) {
        var optional = gameScoreService.findScore(g.getId());

        if (optional.isPresent()) {
            var score = optional.get();
            g.setAwayScore(score.getAwayScore());
            g.setHomeScore(score.getHomeScore());
        } else {
            g.setAwayScore(null);
            g.setHomeScore(null);
        }
    }

    private List<NflGame> filterByThurs(boolean includeThurs, List<NflGame> weekGames) {
        if (includeThurs) return weekGames;
        else
            return weekGames.stream()
                    .filter(g -> !isThursday(g.getGameTime()))
                    .filter(g -> !isFriday(g.getGameTime()))
                    .toList();
    }

    private boolean isThursday(Instant instant) {
        return isDayOfWeek(instant, DayOfWeek.THURSDAY);
    }

    private boolean isFriday(Instant instant) {
        return isDayOfWeek(instant, DayOfWeek.FRIDAY);
    }

    private boolean isDayOfWeek(Instant instant, DayOfWeek dayOfWeek) {
        ZonedDateTime zdt = instant.atZone(ZoneId.of("America/New_York"));
        return zdt.getDayOfWeek() == dayOfWeek;
    }

    public List<NflGame> getGameList() {
        gameList.parallelStream().forEach(this::addScores);
        return gameList;
    }

    public List<NflGame> getWeeklyGames(NflWeek week) {
        var n = gameList.stream().filter(g -> g.getWeek().equals(week.getWeekNum())).toList();
        n.parallelStream().forEach(this::addScores);
        return n;
    }

    public List<NflGame> getWeeklyGamesNotStarted(NflWeek week) {
        return gameList.stream()
                .filter(g -> g.getWeek() != null && g.getWeek().equals(week.getWeekNum()))
                .filter(g -> g.getGameTime().isAfter(Instant.now()))
                .toList();
    }
}
