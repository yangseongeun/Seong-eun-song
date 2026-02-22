package mp3;

import javafx.application.Platform;

public class MP3_PlayerMain {
    public static void main(String[] args) {
        Platform.startup(() -> {}); // JavaFX 플랫폼 초기화
        String subject = "Yang's MP3 Player";
        int width = 450;
        int height = 300;
        MP3_Player mp3 = new MP3_Player(subject, width, height);
    }
}
