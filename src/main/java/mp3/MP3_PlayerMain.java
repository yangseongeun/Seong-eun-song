package mp3;

import javafx.application.Platform;
import mp3.controller.PlayerController;
import mp3.domain.Playlist;
import mp3.service.MediaPlayBackEngine;
import mp3.ui.MP3PlayerFrame;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MP3_PlayerMain {
    public static void main(String[] args) {
        // [기능] JavaFX FileChooser 사용을 위한 초기화
        // FileChooser를 Swing 프로젝트 안에서 쓰기 위해 한 번 초기화한다.
        Platform.startup(() -> {});

        // [기능] Swing UI는 반드시 EDT(Event Dispatch Thread)에서 실행
        SwingUtilities.invokeLater(() -> {

            // [기능] View 생성
            MP3PlayerFrame view = new MP3PlayerFrame("Yang's MP3 Player", 450, 600);

            // [기능] 재생 목록 상태 관리 객체
            Playlist playlist = new Playlist();

            // [기능] 실제 MP3 재생 엔진
            MediaPlayBackEngine engine = new MediaPlayBackEngine();

            // [기능] 이벤트 제어 담당 컨트롤러
            PlayerController controller = new PlayerController(view, playlist, engine);

            // [기능] 버튼 / 메뉴 / 리스트 이벤트를 컨트롤러에 연결
            view.bind(controller, controller, controller);

            // 창의 X 버튼으로 종료할 때도 재생 스레드와 리소스를 정리하도록 추가
            view.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    engine.shutdown();
                }
            });

            // 화면 표시 책임(main)
            view.setVisible(true);
        });
    }
}
