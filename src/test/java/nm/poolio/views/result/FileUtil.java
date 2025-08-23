package nm.poolio.views.result;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public interface FileUtil {
    default String readFromFileToString(String filePath) throws IOException {
        File resource = new ClassPathResource(filePath).getFile();
        byte[] byteArray = Files.readAllBytes(resource.toPath());
        return new String(byteArray);
    }
}
