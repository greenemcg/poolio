package nm.poolio;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import nm.poolio.data.AuditorAwareImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@Theme(value = "poolio")
@ServletComponentScan
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class Application implements AppShellConfigurator {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  AuditorAware<String> auditorProvider() {
    return new AuditorAwareImpl();
  }

  @Override
  public void configurePage(AppShellSettings settings) {
    settings.addMetaTag("author", "Michael C Greene greenemcg@gmail.com");
    settings.addLink("shortcut icon", "icons/favicon.ico");
  }
}
