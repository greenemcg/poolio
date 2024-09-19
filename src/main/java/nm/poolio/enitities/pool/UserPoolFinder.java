package nm.poolio.enitities.pool;

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import java.util.List;
import java.util.Optional;
import nm.poolio.vaadin.PoolioNotification;
import org.springframework.util.CollectionUtils;

public interface UserPoolFinder extends PoolioNotification {
  default List<Pool> findPoolsForUser(List<PoolIdName> poolIdNames, PoolService service) {
    if (CollectionUtils.isEmpty(poolIdNames)) return List.of();

    return poolIdNames.stream()
        .map(idName -> service.get(idName.getId()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  default Notification createNoPoolNotification() {
    Div text =
        new Div(
            new Text("Your Account is not linked to any Poolio pools."),
            new HtmlComponent("br"),
            new Text("Contact your admin to join a pool."));
    Notification notification = createErrorNotification(text);
    notification.addDetachListener(
        openedChangeEvent -> {
          UI.getCurrent().navigate("home");
        });

    notification.open();
    return notification;
  }

  default Notification createNoFundsNotification() {
    Div text =
        new Div(
            new Text("Your Account has no funds"),
            new HtmlComponent("br"),
            new Text("Contact your admin how to add some..."));
    Notification notification = createErrorNotification(text);
    notification.addDetachListener(
        openedChangeEvent -> {
          UI.getCurrent().navigate("home");
        });

    notification.open();
    return notification;
  }
}
