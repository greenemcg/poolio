package nm.poolio.vaadin;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.vaadin.lineawesome.LineAwesomeIcon;

import javax.annotation.Nullable;

import static org.vaadin.lineawesome.LineAwesomeIcon.SAVE_SOLID;
import static org.vaadin.lineawesome.LineAwesomeIcon.WINDOW_CLOSE_SOLID;

public interface PoolioDialog {

    // if clickListener uses a service call this in constructor not on member var init
    default void createDialog(
            Dialog dialog,
            @Nullable ComponentEventListener<ClickEvent<Button>> clickListener,
            Component... components) {

        VerticalLayout layout = createDialogLayout(components);
        dialog.add(layout);
        dialog.getFooter().add(new Button("Cancel", WINDOW_CLOSE_SOLID.create(), e -> dialog.close()));

        if (clickListener != null) {
            Button saveButton = new Button("Save", SAVE_SOLID.create(), clickListener);
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dialog.getFooter().add(saveButton);
        }
    }

    default VerticalLayout createDialogLayout(Component... components) {
        var layout = new VerticalLayout(components);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        layout.getStyle().set("width", "18rem").set("max-width", "100%");

        return layout;
    }

    default Span createDialogTitlespan(String title, LineAwesomeIcon icon) {
        Span span = new Span();
        span.add(icon.create());
        span.add(" " + title);

        return span;
    }
}
