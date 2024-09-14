package nm.poolio.views.user;

import static nm.poolio.utils.VaddinUtils.ADMIN_ICON;
import static nm.poolio.utils.VaddinUtils.AMOUNT_ICON;
import static nm.poolio.utils.VaddinUtils.CREDIT_ICON;
import static nm.poolio.utils.VaddinUtils.EMAIL_ICON;
import static nm.poolio.utils.VaddinUtils.PAY_AS_YOU_GO;
import static nm.poolio.utils.VaddinUtils.PHONE_ICON;
import static nm.poolio.utils.VaddinUtils.POOL_ICON;
import static nm.poolio.utils.VaddinUtils.USER_ICON;
import static nm.poolio.utils.VaddinUtils.USER_NAME_ICON;
import static nm.poolio.utils.VaddinUtils.createIconSpan;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.List;
import nm.poolio.data.User;
import nm.poolio.enitities.pool.PoolIdName;
import nm.poolio.enitities.transaction.PoolioTransactionService;
import nm.poolio.vaadin.PoolioGrid;
import org.springframework.util.CollectionUtils;

public interface UserGrid extends PoolioGrid<User> {
  PoolioTransactionService getPoolioTransactionService();

  private String getFundsString(User u) {
    var funds = getPoolioTransactionService().getFunds(u);
    return "$" + funds;
  }

  default void decoratePoolGrid() {
    getGrid()
        .addColumn(new ComponentRenderer<>(this::createUserComponent))
        .setHeader(createIconSpan(USER_ICON, "User"))
        .setComparator(User::getName)
        .setAutoWidth(true);

    createColumn(User::getUserName, createIconSpan(USER_NAME_ICON, "Username"))
        .setComparator(User::getUserName)
        .setTextAlign(ColumnTextAlign.START);

    createColumn(User::getFunds, createIconSpan(AMOUNT_ICON, "Funds"))
        .setComparator(User::getFunds);

    createColumn(User::getCreditAmount, createIconSpan(CREDIT_ICON, "Credit Amt"))
        .setComparator(User::getCreditAmount);

    getGrid()
        .addColumn(new ComponentRenderer<>(u -> createUserPoolComponent(u.getPoolIdNames())))
        .setHeader(createIconSpan(POOL_ICON, "Pools"))
        .setAutoWidth(true);

    createColumn(User::getPhoneFmt, createIconSpan(PHONE_ICON, "Phone"));
    createColumn(User::getAdmin, createIconSpan(ADMIN_ICON, "Admin")).setComparator(User::getAdmin);
    createColumn(User::getEmail, createIconSpan(EMAIL_ICON, "Email"));

    createColumn(User::isPayAsYouGo, createIconSpan(PAY_AS_YOU_GO, "Pay As You Go"))
        .setComparator(User::isPayAsYouGo);
  }

  default Component createUserPoolComponent(List<PoolIdName> poolIdNames) {

    if (CollectionUtils.isEmpty(poolIdNames)) return new Span("");
    else {
      var names = poolIdNames.stream().map(PoolIdName::getName).toList();
      return new Span(String.join(", ", names));
    }
  }
}
