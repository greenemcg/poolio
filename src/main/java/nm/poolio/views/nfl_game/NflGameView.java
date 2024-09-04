package nm.poolio.views.nfl_game;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.enitities.score.GameScore;
import nm.poolio.enitities.score.GameScoreService;
import nm.poolio.model.NflGame;
import nm.poolio.security.AuthenticatedUser;
import nm.poolio.services.NflGameScorerService;
import nm.poolio.services.NflGameService;
import nm.poolio.views.MainLayout;

@PageTitle("Nfl Games \uD83C\uDFC8")
@Route(value = "nflGame", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "USER"})
@Slf4j
public class NflGameView extends VerticalLayout implements NflGameGrid {
  private final NflGameService service;
  private final GameScoreService gameScoreService;
  private final AuthenticatedUser authenticatedUser;

  private final NflGameScorerService nflGameScorerService;

  @Getter Grid<NflGame> grid = createGrid(NflGame.class);

  public NflGameView(
          NflGameService service,
          GameScoreService gameScoreService,
          AuthenticatedUser authenticatedUser, NflGameScorerService nflGameScorerService) {
    this.service = service;
    this.gameScoreService = gameScoreService;
    this.authenticatedUser = authenticatedUser;
      this.nflGameScorerService = nflGameScorerService;
      setHeight("100%");

    decorateGrid();

    setPadding(true);
    add(new H3("Nfl Games"));
    add(grid);
  }

  private void decorateGrid() {
    decoratePoolGrid();
    grid.setItems(nflGameScorerService.getAllGames());

    var optional = authenticatedUser.get();
    boolean isAdmin = optional.isPresent() && optional.get().getAdmin();

    if (isAdmin) {
      createScoreEditorInGrid();
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
                  Button editButton = new Button("Edit");
                  editButton.addClickListener(
                      e -> {
                        if (editor.isOpen()) editor.cancel();
                        grid.getEditor().editItem(nflGame);
                        binder.setBean(nflGame);
                      });
                  return editButton;
                })
            .setWidth("150px")
            .setFlexGrow(0);

    NumberField homeScoreField = getNumberField();
    binder.forField(homeScoreField).bind(NflGame::getHomeScoreDouble, NflGame::setHomeScoreDouble);
    grid.getColumns().get(3).setEditorComponent(homeScoreField);

    NumberField awayScoreField = getNumberField();
    binder.forField(awayScoreField).bind(NflGame::getAwayScoreDouble, NflGame::setAwayScoreDouble);
    grid.getColumns().get(1).setEditorComponent(awayScoreField);

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

  private NumberField getNumberField() {
    NumberField homeScoreField = new NumberField();
    homeScoreField.setStepButtonsVisible(false);
    homeScoreField.setMin(0);
    homeScoreField.setMax(100);
    homeScoreField.setWidthFull();
    return homeScoreField;
  }

  private void setGameScore(NflGame bean) {
    GameScore score = findOrCreateGameScore(bean);

    score.setAwayScore(bean.getAwayScore());
    score.setHomeScore(bean.getHomeScore());

    gameScoreService.save(score);



    // clear caches;


   // grid.setItems(service.getGameList());
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
}
