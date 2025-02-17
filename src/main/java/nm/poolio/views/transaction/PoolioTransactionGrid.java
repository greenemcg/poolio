package nm.poolio.views.transaction;

import static nm.poolio.utils.VaddinUtils.*;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;
import nm.poolio.data.AbstractEntity;
import nm.poolio.data.User;
import nm.poolio.enitities.transaction.PoolioTransaction;
import nm.poolio.vaadin.PoolioGrid;
import org.vaadin.lineawesome.LineAwesomeIcon;

public interface PoolioTransactionGrid extends PoolioGrid<PoolioTransaction> {
  default @Nullable User getUser() {
    return null;
  }

  void setTemporalAmountColumn(Column<PoolioTransaction> column);

  void setSequenceColumn(Column<PoolioTransaction> column);

  void setPayAsYouGoColumn(Column<PoolioTransaction> column);

  private Renderer<PoolioTransaction> createCreditUserRenderer() {
    return LitRenderer.<PoolioTransaction>of(getUserTemplateExpression())
        .withProperty("pictureUrl", pojo -> createUserPictureUrl(pojo.getCreditUser()))
        .withProperty("fullName", t -> t.getCreditUser().getName());
  }

  private Renderer<PoolioTransaction> createDebitUserRenderer() {
    return LitRenderer.<PoolioTransaction>of(getUserTemplateExpression())
        .withProperty("pictureUrl", pojo -> createUserPictureUrl(pojo.getDebitUser()))
        .withProperty("fullName", t -> t.getDebitUser().getName());
  }

  private Renderer<PoolioTransaction> createPayAsYouGoRenderer() {
    return LitRenderer.<PoolioTransaction>of(getUserTemplateExpression())
        .withProperty(
            "pictureUrl",
            pojo -> createUserPictureUrl(pojo == null ? null : pojo.getPayAsYouGoUser()))
        .withProperty(
            "fullName",
            t ->
                ((t == null || t.getPayAsYouGoUser() == null)
                    ? null
                    : t.getPayAsYouGoUser().getName()));
  }

  default void decorateTransactionGrid() {

    var sequenceColumn =
        createColumn(
                PoolioTransaction::getSequence, createIconSpan(LineAwesomeIcon.HASHTAG_SOLID, ""))
            .setComparator(PoolioTransaction::getSequence);
    setSequenceColumn(sequenceColumn);

    getGrid()
        .addColumn(
            new ComponentRenderer<>(
                transaction -> createUserComponent(transaction.getCreditUser())))
        .setHeader(createIconSpan(USER_ICON, "Credit", LineAwesomeIcon.MINUS_SOLID))
        .setAutoWidth(true)
        .setComparator(t -> t.getCreditUser().getName());

    getGrid()
        .addColumn(
            new ComponentRenderer<>(transaction -> createUserComponent(transaction.getDebitUser())))
        .setHeader(createIconSpan(USER_ICON, "Debit", LineAwesomeIcon.PLUS_SOLID))
        .setAutoWidth(true)
        .setComparator(t -> t.getDebitUser().getName());

    if (getUser() == null || getUser().isPayAsYouGo()) {
      var c =
          getGrid()
              .addColumn(new ComponentRenderer<>(t -> createUserComponent(t.getPayAsYouGoUser())))
              .setHeader(createIconSpan(LineAwesomeIcon.USER_TIE_SOLID, "Player (PayAsYouGo)"))
              .setAutoWidth(true);
      setPayAsYouGoColumn(c);
    }

    createColumn(PoolioTransaction::getAmount, createIconSpan(AMOUNT_ICON, "Amt"))
        .setComparator(PoolioTransaction::getAmount);

    var amountColumn =
        createColumn(
                PoolioTransaction::getTemporalAmount,
                createIconSpan(LineAwesomeIcon.HISTORY_SOLID, "Sum"))
            .setComparator(PoolioTransaction::getTemporalAmount);
    setTemporalAmountColumn(amountColumn);

    createColumn(PoolioTransaction::getType, createIconSpan(MONEY_TYPE_ICON, "Type"))
        .setTextAlign(ColumnTextAlign.START)
        .setComparator(t -> t.getType().name());

    getGrid()
        .addColumn(
            new LocalDateTimeRenderer<>(
                PoolioTransaction::getCreatedLocalDateTime,
                () -> DateTimeFormatter.ofPattern("MMM d, h:mm a")))
        .setHeader(createIconSpan(CREATED_ICON, "Created (EST)"))
        .setAutoWidth(true)
        .setComparator(AbstractEntity::getCreatedDate);

    createColumn(PoolioTransaction::getNote, createIconSpan(NOTES_ICON, "Note"))
        .setAutoWidth(true)
        .setTooltipGenerator(PoolioTransaction::getNote)
        .setTextAlign(ColumnTextAlign.START)
        .getElement()
        .getStyle()
        .set("font-size", "small");
  }
}
