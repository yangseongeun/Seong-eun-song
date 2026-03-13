package mp3;

import javafx.application.Platform;
import mp3.controller.PlayerController;
import mp3.domain.Playlist;
import mp3.service.JLayerPlayBackEngine;
import mp3.ui.MP3PlayerFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MP3_PlayerMain {
    public static void main(String[] args) {
        // JavaFX FileChooser 사용을 위한 초기화
        Platform.startup(() -> {});

        // Swing UI는 EDT에서 실행
        SwingUtilities.invokeLater(() -> {
            MP3PlayerFrame view = new MP3PlayerFrame("Yang's MP3 Player", 450, 300);
            Playlist playlist = new Playlist();
            JLayerPlayBackEngine engine = new JLayerPlayBackEngine();
            PlayerController controller = new PlayerController(view, playlist, engine);

            // Controller 연결
            view.bind(controller, controller, controller);

            // 창 닫기 시 재생 스레드/리소스 정리
            view.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    engine.shutdown();
                }
            });

            view.setVisible(true);
        });
    }
}
