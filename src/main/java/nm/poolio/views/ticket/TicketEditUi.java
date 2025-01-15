package nm.poolio.views.ticket;

import static nm.poolio.utils.VaddinUtils.TIE_BREAKER_ICON;
import static org.vaadin.lineawesome.LineAwesomeIcon.SAVE_SOLID;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import java.util.Objects;
import javax.annotation.Nullable;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.Pool;
import nm.poolio.enitities.pool.PoolIdName;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.ticket.Ticket;
import nm.poolio.enitities.ticket.TicketService;
import nm.poolio.model.NflGame;
import nm.poolio.model.enums.NflTeam;
import nm.poolio.model.enums.NflWeek;
import nm.poolio.model.enums.OverUnder;
import nm.poolio.vaadin.PoolioAvatar;
import nm.poolio.vaadin.PoolioBadge;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.vaadin.PoolioNotification;

public interface TicketEditUi extends PoolioNotification, PoolioDialog, PoolioAvatar, PoolioBadge {
  void setErrorFound(boolean errorFound);

  PoolService getPoolService();

  TicketService getTicketService();

  HasComponents getDialogHasComponents();

  default RadioButtonGroup<OverUnder> createOverUnderField(NflGame g, Ticket ticket) {
    // HorizontalLayout layout = new HorizontalLayout();
    RadioButtonGroup<OverUnder> radioGroup = new RadioButtonGroup<>();
    radioGroup.setItems(OverUnder.OVER, OverUnder.UNDER);
    radioGroup.setRequired(true);
    radioGroup.setLabel("Total Score " + g.getOverUnder());

    var value = ticket.getSheet().getOverUnderPicks().get(g.getId());

    if( value != null) {
      radioGroup.setValue(value);
    }

   // radioGroup.addValueChangeListener(c -> setOverUnderValue(ticket, c.getValue(), g.getId()));

    return radioGroup;
    //   layout.add(radioGroup);

//    return layout;
  }


  default Long convertTicketIdParameter(String ticketIdParameter) {
    return convertIdParameter(ticketIdParameter, "Invalid ticketId: ");
  }

  default Long convertPoolIdParameter(String poolIdParameter) {
    return convertIdParameter(poolIdParameter, "Invalid poolId: ");
  }

  private @Nullable Long convertIdParameter(String queryParameter, String message) {
    try {
      return Long.valueOf(queryParameter);
    } catch (NumberFormatException ignored) {
      setErrorFound(true);
      creatErrorDialogAndGoHome(message + queryParameter);
      return null;
    }
  }

  default void setTicketGameValue(Ticket ticket, NflTeam value, String id) {
    if (ticket.getSheet().getGamePicks().containsKey(id)) {
      ticket.getSheet().getGamePicks().put(id, value);
    }
  }


  default IntegerField createTieBreakerField(Ticket ticket) {
    IntegerField tbField = new IntegerField();
    tbField.setRequired(true);
    tbField.setValue(ticket.getSheet().getTieBreaker());
    tbField.setStepButtonsVisible(false); // does not work on IOS
    tbField.setMin(0);
    tbField.setMax(150);
    tbField.addValueChangeListener(e -> ticket.getSheet().setTieBreaker(e.getValue()));

    tbField.setLabel("Tie Breaker (last game)");
    tbField.setPrefixComponent(TIE_BREAKER_ICON.create());

    return tbField;
  }

  default Button createSubmitButton(ComponentEventListener<ClickEvent<Button>> listener) {
    Button submitButton = new Button("Save Ticket");
    submitButton.setPrefixComponent(SAVE_SOLID.create());
    submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submitButton.addClickListener(listener);

    return submitButton;
  }

  default Ticket createTicket(Pool pool, User player) {
    return createTicket(pool, player, pool.getWeek());
  }

  default Ticket createTicket(Pool pool, User player, NflWeek week) {
    Ticket ticket = new Ticket();
    ticket.setPool(pool);
    ticket.setPlayer(player);
    ticket.setSeason(pool.getSeason());
    ticket.setWeek(week);
    return ticket;
  }

  private void creatErrorDialogAndGoHome(String message) {
    setErrorFound(true);

    Dialog dialog = new Dialog();
    createDialog(dialog, null, new Div(message));
    getDialogHasComponents().add(dialog);
    dialog.open();

    dialog.addDialogCloseActionListener(e -> UI.getCurrent().navigate("home"));
  }

  private void createNotFoundInDbDialogAndGoHome(HasComponents hasComponents) {
    setErrorFound(true);
    hasComponents.add(createErrorNotificationAndGoHome("Cannot find pool in the Poolio Database"));
  }

  default @Nullable Pool findPool(User player, Long poolId) {
    var optionalId =
        player.getPoolIdNames().stream()
            .map(PoolIdName::getId)
            .filter(id -> Objects.equals(id, poolId))
            .findFirst();

    // var optional = getPoolService().findPoolForUser(player, poolId);

    if (optionalId.isPresent()) {
      var optional = getPoolService().get(optionalId.get());
      if (optional.isPresent()) return optional.get();
      else {
        showPoolNotFoundErrorDialog();
        return null;
      }
    } else {
      showPoolNotFoundErrorDialog();
      return null;
    }
  }

  private void showPoolNotFoundErrorDialog() {
    setErrorFound(true);
    getDialogHasComponents()
        .add(createErrorNotification(new Span("Cannot find pool in the Poolio Database")));
  }

  default @Nullable Pool findPoolWithQueryParam(String poolIdParameter, User player) {
    var poolId = convertPoolIdParameter(poolIdParameter);
    return findPool(player, poolId);
  }

  default @Nullable Ticket processTicketIdParameter(String ticketIdParameter, User player) {
    var ticketId = convertTicketIdParameter(ticketIdParameter);
    return findTicket(player, ticketId);
  }

  private @Nullable Ticket findTicket(User player, Long ticketId) {
    var optional = getTicketService().findTicketForUser(player, ticketId);

    if (optional.isPresent()) {
      return optional.get();
    } else {
      setErrorFound(true);
      getDialogHasComponents()
          .add(createErrorNotification(new Span("Cannot find ticket in the Poolio Database")));
      return null;
    }
  }

  default HorizontalLayout createHeaderBadgesTop(Pool pool, Ticket ticket) {
    var layout = new HorizontalLayout();

    createPoolBadge(pool, layout);
    createTicketBadge(ticket, layout);

    return layout;
  }

  default HorizontalLayout createHeaderBadgesBottom(Ticket ticket) {
    var layout = new HorizontalLayout();
    layout.add(createBadge(new Span("TieBreaker: " + ticket.getTieBreaker())));
    layout.add(createBadge(new Span("Rank: " + ticket.getRankString())));
    layout.add(createBadge(new Span("Score: " + ticket.getScoreString())));

    return layout;
  }
}
