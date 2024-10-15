package nm.poolio.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.ValueProvider;
import javax.annotation.Nullable;
import nm.poolio.data.User;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import org.apache.commons.lang3.ArrayUtils;

public interface PoolioGrid<T> extends PoolioAvatar {
  Grid<T> getGrid();

  default @Nullable String createUserPictureUrl(@Nullable User user) {
    if (user == null) return null;

    return ArrayUtils.isEmpty(user.getImageResource()) ? "" : "/image?type=user&id=" + user.getId();
  }

  default Component createUserComponent(@Nullable User u) {
    if (u == null) return new Span();

    HorizontalLayout layout = new HorizontalLayout();
    layout.setAlignItems(Alignment.CENTER);
    layout.setPadding(false);
    layout.setSpacing(false);
    layout.add(createUserAvatar(u, AvatarVariant.LUMO_SMALL));
    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.add(new Span(u.getName()));
    layout.add(verticalLayout);

    return layout;
  }

  default Grid<T> createGrid(Class<T> type) {
    Grid<T> grid = new Grid<>(type, false);
    grid.addClassName("styling-header-footer");
    grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    grid.addThemeVariants(GridVariant.LUMO_COMPACT);
    grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);

    return grid;
  }

  default Column<T> createColumn(ValueProvider<T, ?> valueProvider, Component headerComponent) {
    return getGrid()
        .addColumn(valueProvider)
        .setHeader(headerComponent)
        .setAutoWidth(true)
        .setTextAlign(ColumnTextAlign.CENTER);
  }

  default String getUserTemplateExpression() {
    return """
                <vaadin-horizontal-layout style="align-items: center;" theme="spacing">\
                <vaadin-avatar colorIndex="2" img="${item.pictureUrl}" name="${item.fullName}" alt="User avatar"></vaadin-avatar>\
                  <vaadin-vertical-layout style="line-height: var(--lumo-line-height-s);">\
                    <div>\
                           ${item.fullName}\
                            <div style="font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);">${item.extraData}</div>\
                          </div>\
                  </vaadin-vertical-layout>\
                </vaadin-horizontal-layout>""";
  }

  default Component createTeamComponent(NflTeam team, NflGame game) {
    Span span = new Span();
    span.add(createNflTeamAvatar(team, AvatarVariant.LUMO_XSMALL));
    span.add(" " + team.name());

    span.setTitle(team.getFullName());

    var winningTeam = game.findWinner();

    if (winningTeam != null && winningTeam != NflTeam.TBD) {
      if (winningTeam == team) {
        span.getStyle().set("font-weight", "bold");
      } else {
        span.getStyle().set("text-decoration", "line-through");
      }
    }

    return span;
  }
}
