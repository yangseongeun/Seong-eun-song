package mp3;

import javafx.application.Platform;
import mp3.controller.PlayerController;
import mp3.domain.Playlist;
import mp3.service.JLayerPlayBackEngine;
import mp3.ui.MP3PlayerFrame;

public class MP3_PlayerMain {
    public static void main(String[] args) {
        Platform.startup(() -> {}); // JavaFX 플랫폼 초기화

        MP3PlayerFrame view = new MP3PlayerFrame("Yang's MP3 Player", 450, 300);
        Playlist playlist = new Playlist();
        JLayerPlayBackEngine engine  = new JLayerPlayBackEngine();
        PlayerController controller = new PlayerController(view, playlist, engine);

        view.bind(controller, controller, controller);
    }
}
