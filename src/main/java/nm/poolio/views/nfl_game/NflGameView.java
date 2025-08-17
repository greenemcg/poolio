package nm.poolio.views.nfl_game;

import static nm.poolio.utils.VaddinUtils.SILLY_QUESTION;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.enitities.score.GameScore;
import nm.poolio.enitities.score.GameScoreService;
import nm.poolio.model.NflGame;
import nm.poolio.push.Broadcaster;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameScorerService;
import nm.poolio.services.NflGameService;
import nm.poolio.vaadin.PoolioDialog;
import nm.poolio.views.MainLayout;
import org.springframework.util.CollectionUtils;

@PageTitle("Nfl Games \uD83C\uDFC8")
@Route(value = "nflGame", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "USER"})
@Slf4j
public class NflGameView extends VerticalLayout implements NflGameGrid, PoolioDialog {
  private final NflGameService service;
  private final GameScoreService gameScoreService;
  private final AuthenticatedUser authenticatedUser;
  private final PoolService poolService;
  private final NflGameScorerService nflGameScorerService;
  Dialog sillyDialog = new Dialog();
  @Getter Grid<NflGame> grid = createGrid(NflGame.class);

  public NflGameView(
      NflGameService service,
      GameScoreService gameScoreService,
      AuthenticatedUser authenticatedUser,
      PoolService poolService,
      NflGameScorerService nflGameScorerService) {
    this.service = service;
    this.gameScoreService = gameScoreService;
    this.authenticatedUser = authenticatedUser;
    this.poolService = poolService;
    this.nflGameScorerService = nflGameScorerService;
    setHeight("100%");

    decorateGrid();

    setPadding(true);
    add(new H3("Nfl Games"));
    add(grid);
  }

  private void decorateGrid() {
    decoratePoolGrid();
    var games = nflGameScorerService.getAllGames();

    grid.setItems(games);

    var optionalUser = authenticatedUser.get();
    boolean isAdmin = optionalUser.isPresent() && optionalUser.get().getAdmin();

    if (isAdmin) {
      createScoreEditorInGrid();
    }

    if (optionalUser.isPresent()) {
      var player = optionalUser.get();

      if (!CollectionUtils.isEmpty(player.getPoolIdNames())) {
        var optionalPool = poolService.get(player.getPoolIdNames().getFirst().getId());

        if (optionalPool.isPresent()) {

          var week = optionalPool.get().getWeek().getWeekNum();
          var optionalGame = games.stream().filter(g -> g.getWeek() == week).findFirst();

          AtomicInteger atomicInteger = new AtomicInteger(0);

          for (NflGame g : games) {
            if (g.getWeek() == week) break;
            else atomicInteger.set(atomicInteger.get() + 1);
          }

          // wont work unless continue is in url
          grid.select(games.get(atomicInteger.get()));
          grid.scrollToIndex(atomicInteger.get());

          grid.getElement()
              .executeJs(
                  "setTimeout(function() { $0.scrollToIndex($1) })",
                  grid.getElement(),
                  atomicInteger.get());
        }
      }
    }
  }

  // this method may never get refactored
  private void createScoreEditorInGrid() {
    Editor<NflGame> editor = grid.getEditor();

    Binder<NflGame> binder = new Binder<>(NflGame.class);
    editor.setBinder(binder);
    editor.setBuffered(false);

    Grid.Column<NflGame> editColumn =
        grid.addComponentColumn(
                nflGame -> {
                  HorizontalLayout layout = new HorizontalLayout();
                  layout.setPadding(false);
                  layout.setSpacing(true);

                  if (!CollectionUtils.isEmpty(nflGame.getSillies())) {
                    Button sillyButton = new Button("Sillies", SILLY_QUESTION.create());
                    sillyButton.addClickListener(e -> openDialog(nflGame));
                    layout.add(sillyButton);
                  }

                  Button editButton = new Button("Edit");
                  editButton.addClickListener(
                      e -> {
                        if (editor.isOpen()) editor.cancel();
                        grid.getEditor().editItem(nflGame);
                        binder.setBean(nflGame);
                      });
                  layout.add(editButton);

                  return layout;
                })
            .setWidth("150px")
            .setFlexGrow(0);

    NumberField homeScoreField = createNumberIntField();
    binder.forField(homeScoreField).bind(NflGame::getHomeScoreDouble, NflGame::setHomeScoreDouble);
    grid.getColumns().get(3).setEditorComponent(homeScoreField);

    NumberField awayScoreField = createNumberIntField();
    binder.forField(awayScoreField).bind(NflGame::getAwayScoreDouble, NflGame::setAwayScoreDouble);
    grid.getColumns().get(1).setEditorComponent(awayScoreField);

    NumberField overUnderField = createNumberDoubleField();
    binder.forField(overUnderField).bind(NflGame::getOverUnder, NflGame::setOverUnder);
    grid.getColumns().get(4).setEditorComponent(overUnderField);

    NumberField spreadField = createNumberDoubleField();
    spreadField.setMax(100.0);
    spreadField.setMin(-100.0);
    binder.forField(spreadField).bind(NflGame::getSpread, NflGame::setSpread);
    grid.getColumns().get(5).setEditorComponent(spreadField);

    Button saveButton =
        new Button(
            "Save",
            e -> {
              editor.closeEditor();
              setGameScore(editor.getBinder().getBean());
            });
    Button cancelButton = new Button(VaadinIcon.CLOSE.create(), e -> editor.cancel());
    cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
    HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
    actions.setPadding(false);
    editColumn.setEditorComponent(actions);
  }

  private void openDialog(NflGame nflGame) {
    sillyDialog.removeAll();
    createDialog(sillyDialog, null, createSilliesUi(nflGame));

    sillyDialog.open();
  }

  private NumberField createNumberIntField() {
    NumberField numberField = new NumberField();
    numberField.setStepButtonsVisible(false);
    numberField.setMin(0);
    numberField.setMax(100);
    numberField.setStep(1);
    numberField.setStepButtonsVisible(true);
    numberField.setWidthFull();
    return numberField;
  }

  private NumberField createNumberDoubleField() {
    NumberField numberField = new NumberField();
    numberField.setStepButtonsVisible(false);
    numberField.setMin(0.0);
    numberField.setMax(125.0);
    numberField.setStep(0.5);
    numberField.setStepButtonsVisible(true);
    numberField.setWidthFull();
    return numberField;
  }

  private void setGameScore(NflGame bean) {
    GameScore score = findOrCreateGameScore(bean);

    score.setAwayScore(bean.getAwayScore());
    score.setHomeScore(bean.getHomeScore());

    if (bean.getOverUnder() != null) score.setOverUnder(BigDecimal.valueOf(bean.getOverUnder()));
    else score.setOverUnder(null);

    if (bean.getSpread() != null) score.setSpread(BigDecimal.valueOf(bean.getSpread()));
    else score.setSpread(null);

    gameScoreService.save(score);

    Broadcaster.broadcast("gameScore updated");

    grid.setItems(service.getGameList());
  }

  private Component createSilliesUi(NflGame nflGame) {
    VerticalLayout verticalLayout = new VerticalLayout();

    if (!CollectionUtils.isEmpty(nflGame.getSillies())) {
      nflGame
          .getSillies()
          .forEach(
              silly -> {
                RadioButtonGroup<String> radioGroup = new RadioButtonGroup<>();
                radioGroup.setLabel(silly.getQuestion());
                radioGroup.setItems(silly.getAnswers());

                radioGroup.addValueChangeListener(
                    e -> {
                      if (nflGame.getSillyAnswers() == null) {
                        nflGame.setSillyAnswers(new HashMap<>());
                      }

                      nflGame.getSillyAnswers().put(silly.getId(), e.getValue());
                    });

                String value =
                    nflGame.getSillyAnswers() != null
                        ? nflGame.getSillyAnswers().get(silly.getId())
                        : null;
                if (value != null) {
                  radioGroup.setValue(value);
                }

                verticalLayout.add(radioGroup);
              });
    }

    Button button = new Button("Save", SILLY_QUESTION.create());
    button.addClickListener(e -> saveSillies(nflGame));
    verticalLayout.add(button);

    return verticalLayout;
  }

  private GameScore findOrCreateGameScore(NflGame bean) {
    var optional = gameScoreService.findScore(bean.getId());

    if (optional.isPresent()) {
      return optional.get();
    } else {
      GameScore score;
      score = new GameScore();
      score.setGameId(bean.getId());
      return score;
    }
  }

  private void saveSillies(NflGame nflGame) {
    var gameScore = findOrCreateGameScore(nflGame);
    gameScore.setSillyAnswers(nflGame.getSillyAnswers());
    gameScoreService.save(gameScore);

    sillyDialog.close();
  }
}
