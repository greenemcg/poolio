package nm.poolio.views.transaction;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.views.MainLayout;

@PageTitle("Transactions \uD83D\uDCB8")
@Route(value = "userTransactions", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@Slf4j
public class UserPoolioTransactionView {
}
