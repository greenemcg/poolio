package nm.poolio.vaadin;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public interface PoolioNotification {
  default Notification createRemoveMe(String message) {
    Div text = new Div(new Text(message));
    Notification notification = createErrorNotification(text);
    notification.addDetachListener(openedChangeEvent -> UI.getCurrent().navigate("home"));

    UI.getCurrent().navigate("home");
    return notification;
  }

  private Notification createNotification(
      NotificationVariant type, int duration, HtmlContainer message) {
    Notification notification = new Notification();
    notification.addThemeVariants(type);
    notification.setPosition(Position.TOP_CENTER);
    notification.setDuration(duration);

    Button closeButton = new Button(new Icon("lumo", "cross"));
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.setAriaLabel("Close");
    closeButton.addClickListener(
        event -> {
          notification.close();
          notification.removeAll();
          notification.removeFromParent();
        });

    HorizontalLayout layout = new HorizontalLayout(message, closeButton);
    layout.setAlignItems(Alignment.CENTER);
    notification.add(layout);

    notification.open();

    return notification;
  }

  default Notification createErrorNotification(HtmlContainer message) {
    return createNotification(NotificationVariant.LUMO_ERROR, 0, message);
  }

  default Notification createWarningNotification(HtmlContainer message) {
    return createNotification(NotificationVariant.LUMO_WARNING, 6000, message);
  }

  default Notification createSucessNotification(HtmlContainer message) {
    return createNotification(NotificationVariant.LUMO_SUCCESS, 3000, message);
  }

  default Notification createInfoNotification(HtmlContainer message) {
    return createNotification(NotificationVariant.LUMO_PRIMARY, 1500, message);
  }
}
