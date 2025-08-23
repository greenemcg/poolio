package nm.poolio.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nm.poolio.data.AvatarImageBytes;
import nm.poolio.enitities.pool.PoolService;
import nm.poolio.services.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import java.io.*;
import java.net.URLConnection;
import java.util.Optional;

@Slf4j
@WebServlet(urlPatterns = "/image", name = "DynamicContentServlet")
public class ProfileImageServlet extends HttpServlet {
    private UserService userService;
    private PoolService poolService;

    @Autowired
    private WebApplicationContext context;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        userService = context.getBean(UserService.class);
        poolService = context.getBean(PoolService.class);
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idString = req.getParameter("id");
        String type = req.getParameter("type");

        if (StringUtils.isEmpty(idString) || StringUtils.isEmpty(type)) {
            resp.getWriter().write("");
            resp.setContentLength(0);
            return;
        }

        Long id = Long.valueOf(idString);
        var optional = getBytes(id, type);

        if (optional.isPresent()) {
            byte[] bytes = optional.get().getImageResource();
            InputStream is = new BufferedInputStream(new ByteArrayInputStream(bytes));
            String mimeType = URLConnection.guessContentTypeFromStream(is);

            resp.setContentType(mimeType);
            resp.setContentLength(is.available());
            ServletOutputStream out = resp.getOutputStream();
            BufferedOutputStream bout = new BufferedOutputStream(out);

            int ch = 0;

            while ((ch = is.read()) != -1) bout.write(ch);

            bout.flush();
            bout.close();
            is.close();
        } else {
            resp.getWriter().write("");
            resp.setContentLength(0);
        }
    }

    private Optional<AvatarImageBytes> findPoolBytes(Long id) {
        var op = poolService.get(id);

        if (op.isPresent()) return Optional.of(op.get());
        else return Optional.empty();
    }

    private Optional<AvatarImageBytes> findUserBytes(Long id) {
        String userName = userService.findUserName(id);

        if (userName == null) return Optional.empty();

        var user = userService.findByUserName(userName);

        if (user != null) return Optional.of(user);
        else return Optional.empty();
    }

    private Optional<AvatarImageBytes> getBytes(Long id, String type) {
        return switch (type) {
            case "user" -> findUserBytes(id);
            case "pool" -> findPoolBytes(id);
            default -> Optional.empty();
        };
    }
}
