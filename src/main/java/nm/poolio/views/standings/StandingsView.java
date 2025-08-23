package nm.poolio.views.standings;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.pool.UserPoolFinder;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.TicketScorerService;
import nm.poolio.views.MainLayout;

import java.util.*;
import java.util.stream.IntStream;

@PageTitle("Standings  \uD83E\uDD47")
@Route(value = "standings", layout = MainLayout.class)
@RolesAllowed("USER")
@Slf4j
public class StandingsView extends VerticalLayout implements UserPoolFinder, StandingsGrid {
    private final TicketScorerService ticketScorerService;
    @Getter
    private final Grid<PlayerStandings> grid = createGrid(PlayerStandings.class);
    private Pool pool;

    public StandingsView(
            AuthenticatedUser authenticatedUser,
            PoolService poolService,
            TicketScorerService ticketScorerService) {
        this.ticketScorerService = ticketScorerService;
        setHeight("100%");

        User player = authenticatedUser.get().orElseThrow();
        var pools = findPoolsForUser(player.getPoolIdNames(), poolService);

        if (pools.isEmpty()) {
            add(createNoPoolNotification());
        } else {
            pool = pools.getFirst();
            var map = populateTickets();
            var standings = populateStandings(map);
            grid.setItems(standings);
            decorateGrid();
            add(grid);
        }
    }

    private List<PlayerStandings> populateStandings(Map<User, List<Ticket>> map) {
        List<PlayerStandings> standings = new ArrayList<>();
        map.forEach((user, tickets) -> standings.add(createPlayerStandings(tickets, user)));

        standings.sort(Comparator.comparing(PlayerStandings::getTotalCorrect).reversed());

        return standings;
    }

    PlayerStandings createPlayerStandings(List<Ticket> tickets, User player) {
        var p = new PlayerStandings(tickets, player);
        p.calculateTotals();
        return p;
    }

    private Map<User, List<Ticket>> populateTickets() {
        Map<User, List<Ticket>> map = new LinkedHashMap<>();

        IntStream.range(1, pool.getWeek().getWeekNum())
                .mapToObj(NflWeek::findByWeekNum)
                .map(week -> ticketScorerService.findAndScoreTickets(pool, week))
                .forEach(
                        tickets -> {
                            tickets.forEach(
                                    t -> map.computeIfAbsent(t.getPlayer(), k -> new ArrayList<>()).add(t));
                        });

        return map;
    }
}
