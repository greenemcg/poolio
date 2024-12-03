package nm.poolio.views.standings;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.pool.UserPoolFinder;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.views.MainLayout;

@PageTitle("Standings  \uD83E\uDD47")
@Route(value = "standings", layout = MainLayout.class)
@RolesAllowed("USER")
@Slf4j
public class StandingsView extends VerticalLayout implements UserPoolFinder {
  private final PoolService poolService;

  User player;
  Pool pool;

  public StandingsView(AuthenticatedUser authenticatedUser, PoolService poolService) {
    this.poolService = poolService;
    setHeight("100%");
    add("Standings");

    this.player = authenticatedUser.get().orElseThrow();

    var pools = findPoolsForUser(player.getPoolIdNames(), poolService);

    if (pools.isEmpty()) {
      add(createNoPoolNotification());
    } else {
      pool = pools.getFirst();

      for (int x = 1; x < pool.getWeek().getWeekNum(); x++) {}
    }
  }
}
