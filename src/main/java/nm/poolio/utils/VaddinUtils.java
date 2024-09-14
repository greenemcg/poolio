package nm.poolio.utils;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.vaadin.lineawesome.LineAwesomeIcon;

public class VaddinUtils {

  public static final LineAwesomeIcon SPLIT_ICON = LineAwesomeIcon.COLUMNS_SOLID;
  public static final LineAwesomeIcon MONEY_POT_ICON = LineAwesomeIcon.HAND_HOLDING_USD_SOLID;
  public static final LineAwesomeIcon EMAIL_ICON = LineAwesomeIcon.AT_SOLID;
  public static final LineAwesomeIcon CREDIT_ICON = LineAwesomeIcon.CREDIT_CARD_SOLID;

  public static final LineAwesomeIcon CREATED_ICON = LineAwesomeIcon.CALENDAR_PLUS_SOLID;

  public static final LineAwesomeIcon MONEY_TYPE_ICON = LineAwesomeIcon.MONEY_CHECK_ALT_SOLID;
  public static final LineAwesomeIcon NOTES_ICON = LineAwesomeIcon.STICKY_NOTE_SOLID;
  public static final LineAwesomeIcon TRANSACTION_ICON = LineAwesomeIcon.FILE_INVOICE_DOLLAR_SOLID;

  public static final LineAwesomeIcon TIE_BREAKER_ICON = LineAwesomeIcon.HEART_BROKEN_SOLID;
  public static final LineAwesomeIcon SCORE_ICON = LineAwesomeIcon.TROPHY_SOLID;
  public static final LineAwesomeIcon RANK_ICON = LineAwesomeIcon.LIST_OL_SOLID;

  public static final LineAwesomeIcon RESULTS_ICON = LineAwesomeIcon.FILE_EXCEL_SOLID;
  public static final LineAwesomeIcon GAME_TIME_ICON = LineAwesomeIcon.CLOCK_SOLID;

  public static final LineAwesomeIcon HOME_ICON = LineAwesomeIcon.HOME_SOLID;
  public static final LineAwesomeIcon AWAY_ICON = LineAwesomeIcon.PLANE_ARRIVAL_SOLID;
  public static final LineAwesomeIcon ID_ICON = LineAwesomeIcon.DATABASE_SOLID;

  public static final LineAwesomeIcon USERS_ICON = LineAwesomeIcon.USERS_SOLID;
  public static final LineAwesomeIcon NEW_ICON = LineAwesomeIcon.PLUS_CIRCLE_SOLID;

  public static final LineAwesomeIcon EDIT_ICON = LineAwesomeIcon.EDIT_SOLID;

  public static final LineAwesomeIcon VIEW_ICON = LineAwesomeIcon.EYE_SOLID;
  public static final LineAwesomeIcon GAMES_ICON = LineAwesomeIcon.TABLE_SOLID;

  public static final LineAwesomeIcon STATUS_ICON = LineAwesomeIcon.BELL_SOLID;
  public static final LineAwesomeIcon WEEK_ICON = LineAwesomeIcon.CALENDAR_WEEK_SOLID;

  public static final LineAwesomeIcon TICKET_ICON = LineAwesomeIcon.TICKET_ALT_SOLID;

  public static final LineAwesomeIcon PLAYERS_ICON = LineAwesomeIcon.USER_FRIENDS_SOLID;
  public static final LineAwesomeIcon PLAYER_ICON = LineAwesomeIcon.USER_EDIT_SOLID;

  public static final LineAwesomeIcon USER_ICON = LineAwesomeIcon.USER_SOLID;
  public static final LineAwesomeIcon USER_NAME_ICON = LineAwesomeIcon.USER_TAG_SOLID;
  public static final LineAwesomeIcon ADMIN_ICON = LineAwesomeIcon.USER_SECRET_SOLID;
  public static final LineAwesomeIcon NAME_ICON = LineAwesomeIcon.SIGNATURE_SOLID;
  public static final LineAwesomeIcon PHONE_ICON = LineAwesomeIcon.SMS_SOLID;

  public static final LineAwesomeIcon POOLIO_ICON = LineAwesomeIcon.FOOTBALL_BALL_SOLID;

  public static final LineAwesomeIcon SPREAD_ICON = LineAwesomeIcon.ARROWS_ALT_V_SOLID;
  public static final LineAwesomeIcon BET_ICON = LineAwesomeIcon.COMMENTS_DOLLAR_SOLID;
  public static final LineAwesomeIcon POOL_ICON = LineAwesomeIcon.SWIMMING_POOL_SOLID;
  public static final LineAwesomeIcon LEAGUE_ICON = LineAwesomeIcon.LIST_ALT_SOLID;
  public static final LineAwesomeIcon AMOUNT_ICON = LineAwesomeIcon.COINS_SOLID;
  public static final LineAwesomeIcon PAY_AS_YOU_GO = LineAwesomeIcon.PIGGY_BANK_SOLID;
  public static final LineAwesomeIcon INCLUDE_THURSDAY = LineAwesomeIcon.QUESTION_SOLID;

  public static Span createIconSpan(LineAwesomeIcon icon, String text) {
    Span span = new Span();
    span.add(icon.create());
    span.add(" " + text);
    return span;
  }

  public static Span createIconSpan(LineAwesomeIcon icon, String text, LineAwesomeIcon end) {
    Span span = new Span();
    span.add(icon.create());
    span.add(" " + text + " ");
    span.add(end.create());
    return span;
  }

  public static Span createAdminLabel() {
    return createIconSpan(ADMIN_ICON, "Admin User");
  }

  public static Span createIncludeThursdayLabel() {
    return createIconSpan(INCLUDE_THURSDAY, "Thursday Games");
  }

  public static void decorateIncludeThursdayCheckbox(Checkbox payAsYouGo) {
    payAsYouGo.setLabelComponent(createIncludeThursdayLabel());
  }

  public static void decorateAdminCheckbox(Checkbox admin) {
    admin.setLabelComponent(createAdminLabel());
  }

  public static void decoratePasswordField(PasswordField password) {
    password.setLabel("Password");
    password.setPlaceholder("6 characters minimum.");
    password.setClearButtonVisible(true);
    password.setPrefixComponent(LineAwesomeIcon.KEY_SOLID.create());
    password.setMinLength(6);
    password.setMaxLength(50);
    password.getElement().setAttribute("autocomplete", "off");
  }

  public static void decorateEmailField(EmailField email) {
    email.setPrefixComponent(EMAIL_ICON.create());
    email.setErrorMessage("Enter a valid email address");
    email.setPlaceholder("Used to email results");
    email.setLabel("Notification Email");
  }

  public static void decorateUserNameField(TextField userName) {
    userName.setPlaceholder("Login Name");
    userName.setLabel("User Name");

    userName.setPrefixComponent(USER_NAME_ICON.create());

    userName.setRequiredIndicatorVisible(true);
    userName.setMinLength(2);
    userName.setMaxLength(50);
  }

  public static void decoratePhoneField(TextField phoneField) {
    phoneField.setPlaceholder("Used to Text Results");
    phoneField.setLabel("Phone number");

    phoneField.setPrefixComponent(LineAwesomeIcon.SMS_SOLID.create());

    phoneField.setMinLength(10);
    phoneField.setMaxLength(50);
  }

  public static void decoratePoolAmountField(IntegerField amountField) {
    amountField.setPlaceholder("Ticket Amount");
    amountField.setLabel("Pool Amount");

    amountField.setPrefixComponent(AMOUNT_ICON.create());

    amountField.setMin(1);
    amountField.setMax(250);
  }

  public static void decorateNameField(TextField userName) {
    userName.setLabel("Name (Public)");
    userName.setPlaceholder("Name used for Pools/Bets");
    userName.setPrefixComponent(NAME_ICON.create());
    userName.setRequiredIndicatorVisible(true);
    userName.setMinLength(2);
    userName.setMaxLength(50);
  }
}
