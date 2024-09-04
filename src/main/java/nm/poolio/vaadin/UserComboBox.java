package nm.poolio.vaadin;

import static nm.poolio.utils.VaddinUtils.USER_ICON;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public class UserComboBox {
  @Getter private User selected;

  public void decorate(ComboBox<User> comboBox, List<User> users, String label) {

    // if (!CollectionUtils.isEmpty(users)) users.sort(Comparator.comparing(User::getName));

    comboBox.setLabel(label);
    comboBox.setItems(users);
    comboBox.addValueChangeListener(event -> selected = event.getValue());
    comboBox.setRenderer(createRenderer());
    comboBox.setItemLabelGenerator(User::getName);
    comboBox.getStyle().set("--vaadin-combo-box-overlay-width", "16em");
    comboBox.setPrefixComponent(USER_ICON.create());
  }

  private Renderer<User> createRenderer() {
    String tpl =
        """
                        <div style="display: flex;">\
                          <div>\
                           ${item.name}\
                          </div>\
                        </div>""";

    return LitRenderer.<User>of(tpl)
        .withProperty(
            "pictureUrl",
            pojo ->
                (ArrayUtils.isEmpty(pojo.getImageResource()))
                    ? ""
                    : "/image?type=user&id=" + pojo.getId())
        .withProperty("name", User::getName)
        .withProperty("userName", User::getUserName);
  }
}
