package mp3.domain;
import java.io.File;

public record Track(File file) {
    public String displayname() {
        return file.getName();
    }
}
