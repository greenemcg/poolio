package nm.poolio.views.pool;

import static nm.poolio.utils.VaddinUtils.AMOUNT_ICON;
import static nm.poolio.utils.VaddinUtils.INCLUDE_THURSDAY;
import static nm.poolio.utils.VaddinUtils.LEAGUE_ICON;
import static nm.poolio.utils.VaddinUtils.PAY_AS_YOU_GO;
import static nm.poolio.utils.VaddinUtils.PLAYERS_ICON;
import static nm.poolio.utils.VaddinUtils.PLAYER_ICON;
import static nm.poolio.utils.VaddinUtils.POOL_ICON;
import static nm.poolio.utils.VaddinUtils.STATUS_ICON;
import static nm.poolio.utils.VaddinUtils.WEEK_ICON;
import static nm.poolio.utils.VaddinUtils.createIconSpan;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.vaadin.PoolioGrid;
import org.apache.commons.lang3.ArrayUtils;

public interface PoolGrid extends PoolioGrid<Pool> {
  void openAdminDialog(Pool pool);

  void openPlayersDialog(Pool pool);

  default Renderer<Pool> createPoolRenderer() {
    return LitRenderer.<Pool>of(
            """
                                <vaadin-horizontal-layout style="align-items: center;" theme="spacing">\
                                <vaadin-avatar img="${item.pictureUrl}" name="${item.fullName}" alt="User avatar"></vaadin-avatar>\
                                  <vaadin-vertical-layout style="line-height: var(--lumo-line-height-m);">\
                                    <span> ${item.name} </span>\
                                    <span style="font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);">\
                                      ${item.season}\
                                    </span>\
                                  </vaadin-vertical-layout>\
                                </vaadin-horizontal-layout>
                                """)
        .withProperty(
            "pictureUrl",
            pojo ->
                (ArrayUtils.isEmpty(pojo.getImageResource()))
                    ? ""
                    : "/image?type=pool&id=" + pojo.getId())
        .withProperty("name", Pool::getName)
        .withProperty("season", pojo -> (pojo.getSeason().getDisplay()));
  }

  default void decoratePoolGrid() {
    getGrid()
        .addColumn(createPoolRenderer())
        .setHeader(createIconSpan(POOL_ICON, "Pool"))
        .setAutoWidth(true);

    createColumn(Pool::getWeek, createIconSpan(WEEK_ICON, "Week"));
    createColumn(Pool::getStatus, createIconSpan(STATUS_ICON, "Status"));
    createColumn(Pool::getAmount, createIconSpan(AMOUNT_ICON, "Amount"));

    //    getGrid()
    //        .addColumn(
    //            new ComponentRenderer<>(
    //                Button::new,
    //                (button, pool) -> {
    //                  button.addThemeVariants(
    //                      ButtonVariant.LUMO_ICON,
    //                      ButtonVariant.LUMO_SUCCESS,
    //                      ButtonVariant.LUMO_TERTIARY);
    //                  button.addClickListener(e -> openAdminDialog(pool));
    //                  button.setPrefixComponent(ADMIN_ICON.create());
    //                  button.setText(pool.getAdmins().size() + " Admins");
    //                  button.getElement().getStyle().set("color", "blue");
    //                }))
    //        .setHeader(createIconSpan(ADMIN_ICON, "Admins"))
    //        .setAutoWidth(true);

    createColumn(Pool::getLeague, createIconSpan(LEAGUE_ICON, "League"));
    createColumn(Pool::isIncludeThursday, createIconSpan(INCLUDE_THURSDAY, "Thursday"));

    getGrid()
        .addColumn(
            new ComponentRenderer<>(
                Button::new,
                (button, pool) -> {
                  button.addThemeVariants(
                      ButtonVariant.LUMO_ICON,
                      ButtonVariant.LUMO_SUCCESS,
                      ButtonVariant.LUMO_TERTIARY);
                  button.addClickListener(e -> openPlayersDialog(pool));
                  button.setPrefixComponent(PLAYERS_ICON.create());
                  button.setText(pool.getPlayers().size() + " Players");
                  button.getElement().getStyle().set("color", "blue");
                }))
        .setHeader(createIconSpan(PLAYER_ICON, "Players"))
        .setAutoWidth(true);

    createColumn(Pool::getMaxPlayersPerWeek, createIconSpan(PLAYERS_ICON, "Max Per Week"));

    createColumn(Pool::getPayAsYouGoUserName, createIconSpan(PAY_AS_YOU_GO, "Pay as You Go User"));
  }
}
