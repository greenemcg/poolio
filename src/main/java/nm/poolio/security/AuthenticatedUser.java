package nm.poolio.security;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.User;
import nm.poolio.services.UserService;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.MDC;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class AuthenticatedUser {
  private final UserService userService;
  private final AuthenticationContext authenticationContext;

  public AuthenticatedUser(AuthenticationContext authenticationContext, UserService userService) {
    this.userService = userService;
    this.authenticationContext = authenticationContext;
  }

  @Transactional
  public Optional<User> get() {
    var result =
        authenticationContext
            .getAuthenticatedUser(UserDetails.class)
            .map(userDetails -> userService.findByUserName(userDetails.getUsername()));

    var vaadinSession = VaadinSession.getCurrent();
    Boolean showedLogin = (Boolean) vaadinSession.getAttribute("showedLogin");

    if (result.isPresent() && BooleanUtils.isNotTrue(showedLogin)) {
      MDC.put("userName", result.get().getUserName());
      MDC.put("sessionId", VaadinSession.getCurrent().getSession().getId());
      vaadinSession.setAttribute("showedLogin", Boolean.TRUE);
      log.info("Successfully Logged in");
    }

    return result;
  }

  public void logout() {
    authenticationContext.logout();
  }
}
