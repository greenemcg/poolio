package nm.poolio.views.personform;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import jakarta.annotation.security.RolesAllowed;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.views.MainLayout;

@PageTitle("Person Form")
@Route(value = "person-form", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class PersonFormView extends Composite<VerticalLayout> implements HasUrlParameter<String> {

  public PersonFormView() {
    log.info("View");

    VerticalLayout layoutColumn2 = new VerticalLayout();
    H3 h3 = new H3();
    FormLayout formLayout2Col = new FormLayout();
    TextField textField = new TextField();
    TextField textField2 = new TextField();
    DatePicker datePicker = new DatePicker();
    TextField textField3 = new TextField();
    EmailField emailField = new EmailField();
    TextField textField4 = new TextField();
    HorizontalLayout layoutRow = new HorizontalLayout();
    Button buttonPrimary = new Button();
    Button buttonSecondary = new Button();
    getContent().setWidth("100%");
    getContent().getStyle().set("flex-grow", "1");
    getContent().setJustifyContentMode(JustifyContentMode.START);
    getContent().setAlignItems(Alignment.CENTER);
    layoutColumn2.setWidth("100%");
    layoutColumn2.setMaxWidth("800px");
    layoutColumn2.setHeight("min-content");
    h3.setText("Personal Information");
    h3.setWidth("100%");
    formLayout2Col.setWidth("100%");
    textField.setLabel("First Name");
    textField2.setLabel("Last Name");
    datePicker.setLabel("Birthday");
    textField3.setLabel("Phone Number");
    emailField.setLabel("Email");
    textField4.setLabel("Occupation");
    layoutRow.addClassName(Gap.MEDIUM);
    layoutRow.setWidth("100%");
    layoutRow.getStyle().set("flex-grow", "1");
    buttonPrimary.setText("Save");
    buttonPrimary.setWidth("min-content");
    buttonPrimary.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    buttonSecondary.setText("Cancel");
    buttonSecondary.setWidth("min-content");
    getContent().add(layoutColumn2);
    layoutColumn2.add(h3);
    layoutColumn2.add(formLayout2Col);
    formLayout2Col.add(textField);
    formLayout2Col.add(textField2);
    formLayout2Col.add(datePicker);
    formLayout2Col.add(textField3);
    formLayout2Col.add(emailField);
    formLayout2Col.add(textField4);
    layoutColumn2.add(layoutRow);
    layoutRow.add(buttonPrimary);
    layoutRow.add(buttonSecondary);
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String s) {
    log.info(s);

    Location location = event.getLocation();
    QueryParameters queryParameters = location.getQueryParameters();
    var optional = queryParameters.getSingleParameter("userId");

    Map<String, List<String>> parametersMap = queryParameters.getParameters();

    log.info(parametersMap.toString());
  }
}
