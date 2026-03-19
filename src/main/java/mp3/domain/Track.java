package mp3.domain;

import java.io.File;

public record Track(File file, String title) {

    public static Track from(File file) {
        return new Track(file, file.getName());
    }

    public String displayName() {
        return title;
    }
}
