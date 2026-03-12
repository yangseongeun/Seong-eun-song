package mp3.controller;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mp3.domain.PlaybackSession;
import mp3.domain.PlayerState;
import mp3.domain.Playlist;
import mp3.domain.Track;
import mp3.service.JLayerPlayBackEngine;
import mp3.ui.MP3PlayerFrame;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class PlayerController implements ActionListener, MouseListener, KeyListener {

    private final MP3PlayerFrame view;
    private final Playlist playlist;
    private final JLayerPlayBackEngine engine;
    private PlaybackSession session = PlaybackSession.stopped();

    public PlayerController(MP3PlayerFrame view, Playlist playlist, JLayerPlayBackEngine engine) {
        this.view = view;
        this.playlist = playlist;
        this.engine = engine;
    }

    // ===== Action =====
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == null) return;

        switch (cmd) {
            case "OPEN" -> openFiles();
            case "EXIT" -> exitApp();
            case "INFO" -> showInfo();
            case "PREV" -> prev();
            case "PLAY_PAUSE" -> togglePlayPause();
            case "STOP" -> stop();
            case "NEXT" -> nextManual();
            case "REPEAT" -> cycleRepeat();
            default -> { /* 무시 */ }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            deleteSelected();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getComponent() instanceof JList<?> && e.getClickCount() == 1) {
            int clicked = view.listIndexAt(e.getPoint());
            playlist.setIndex(clicked);
            return;
        }

        if (e.getComponent() instanceof JList<?> && e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            int clicked = view.listIndexAt(e.getPoint());
            if (clicked < 0) return;
            playlist.setIndex(clicked);
            playSelected();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    // ===== Commands =====
    // ===== File Open (kept: JavaFX FileChooser) =====
    public void openFiles() {
        Platform.runLater(() -> {
            // 윈도우 파일 선택기 인스턴스 생성
            FileChooser chooser = new FileChooser();

            // 파일 선택기 창 제목
            chooser.setTitle("mp3 파일 선택하기");

            // MP3 파일만 표시하도록 파일 확장자 필터 설정
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("음악 파일(*.mp3)", "*.mp3"));

            // 다중 선택 기능
            List<File> selected = chooser.showOpenMultipleDialog(new Stage());
            if (selected == null || selected.isEmpty()) return;

            SwingUtilities.invokeLater(() -> {
                DefaultListModel<String> model = view.listModel();
                for (File f : selected) { //선택된 파일 만큼

                    if (f == null) continue;

                    if (playlist.contains(f)) { //배열에 저장된 파일이 중복될 때
                        JOptionPane.showMessageDialog(null, "이미 존재하는 파일입니다.",
                                "알림", JOptionPane.INFORMATION_MESSAGE);
                        continue;
                    }

                    if (!f.getName().toLowerCase().endsWith("mp3")) { //확장자가 mp3가 아닐 때
                        JOptionPane.showMessageDialog(null,
                                "파일 형식이 올바르지 않습니다.\n이 플레이어는 확장자가 mp3인 파일만 지원합니다.",
                                "경고", JOptionPane.WARNING_MESSAGE);
                        continue;
                    }

                    Track track = Track.from(f);
                    playlist.add(track); // 플레이 리스트에 음악 파일 추가
                    model.addElement(track.displayName()); // 재생 목록 UI에 음악 파일 추가

                    if (!engine.isPlaying()) {
                        view.setSubTitleText("음악 파일을 재생하려면 재생 버튼을 누르세요.");
                    }
                }
            });
        });
    }

    public void exitApp() {
        if (view.confirmExit()) {
            engine.shutdown();
            System.exit(0);
        }
    }

    public void showInfo() {
        view.showInfoDialog();
    }

    public void prev() {
        if (playlist.isEmpty()) {
            view.showNoFileError();
            return;
        }
        playlist.previous();
        engine.stop();
        session = session.withState(PlayerState.STOPPED);
        startCurrent();
    }

    public void nextManual() {
        if (playlist.isEmpty()) {
            view.showNoFileError();
            return;
        }
        playlist.nextManual();
        engine.stop();
        session = session.withState(PlayerState.STOPPED);
        startCurrent();
    }

    // 재생-일시정지 버튼이 눌렸을 때 실행되는 메소드
    public void togglePlayPause() {
        if (playlist.isEmpty()) {
            view.showNoFileError();
            return;
        }

        if (engine.isPlaying() && !engine.isPaused()) {
            engine.pause();
            session = session.withState(PlayerState.PAUSED);
            renderSession();
            view.setTitleText("음악 일시 정지됨");
            view.setSubTitleText("재생 버튼을 누르면 이어서 재생됩니다.");
            view.setPlayIcon();
            return;
        }

        if (engine.isPlaying() && engine.isPaused()) {
            engine.resume(
                    () -> SwingUtilities.invokeLater(this::onTrackCompleted),
                    ex -> SwingUtilities.invokeLater(() -> view.showError("재생 오류: " + ex.getMessage()))
            );
            Track track = playlist.currentOrNull();
            if (track != null) {
                view.setTitleText("현재 재생 중인 파일은 " + track.displayName() + " 입니다.");
            }
            view.setSubTitleText("음악 재생 중");
            view.setPauseIcon();
            return;
        }

        playSelectedOrCurrent();
    }

    // 정지 버튼이 눌렸을 때 실행되는 메소드
    public void stop() {
        if (playlist.isEmpty()) { // 파일 리스트가 비었는지 확인하고 비었으면 파일 없음 오류 메시지 출력
            view.showNoFileError();
            return;
        }
        engine.stop();
        session = session.withState(PlayerState.STOPPED);
        renderSession();
        view.setTitleText("Seong Eun Song"); //제목을 "Seong Eun Song"으로 변경
        view.setSubTitleText("음악 정지");
        view.setPlayIcon(); //아이콘을 재생 아이콘으로 변경
    }

    private void playSelectedOrCurrent() {
        if (playlist.isEmpty()) {
            view.showNoFileError();
            return;
        }
        startCurrent();
    }

    private void playSelected() {
        if (playlist.isEmpty()) return;
        engine.stop();
        session = session.withState(PlayerState.STOPPED);
        startCurrent();
    }

    private void startCurrent() {
        Track track = playlist.currentOrNull();
        if (track == null) {
            view.showNoFileError();
            session = PlaybackSession.stopped();
            return;
        }

        session = new PlaybackSession(track, PlayerState.PLAYING);
        renderSession();

        engine.play(
                track.file(),
                () -> SwingUtilities.invokeLater(this::onTrackCompleted),
                ex -> SwingUtilities.invokeLater(() -> {
                    session = session.withState(PlayerState.ERROR);
                    view.showError("재생 오류: " + ex.getMessage());
                    renderSession();
                })
        );
    }

    private void onTrackCompleted() {
        if (playlist.isEmpty()) {
            session = PlaybackSession.stopped();
            resetToIdle("Seong Eun Song", "재생할 파일이 없습니다.");
            return;
        }

        Track next = playlist.nextAuto();
        if (next == null) {
            session = session.withState(PlayerState.STOPPED);
            resetToIdle("Seong Eun Song", "리스트에 있는 모든 음악 재생 끝");
            return;
        }
        startCurrent();
    }

    public void cycleRepeat() {
        playlist.cycleMode();
        view.setRepeatText(playlist.modeLabel());
    }

    private void deleteSelected() {
        int[] selected = view.selectedIndices();
        if (selected == null || selected.length == 0) return;

        DefaultListModel<String> model = view.listModel();

        // domain 상태 정리
        playlist.deleteIndices(selected);

        // UI 모델 삭제(역순)
        for (int i = selected.length - 1; i >= 0; i--) {
            int idx = selected[i];
            if (idx >= 0 && idx < model.size()) model.remove(idx);
        }

        view.repaintPlaylist();
    }

    private void resetToIdle(String title, String subtitle) {
        session = session.withState(PlayerState.STOPPED);
        view.setTitleText(title);
        view.setSubTitleText(subtitle);
        view.setPlayIcon();
    }

    private void renderSession() {
        if (session.isStopped()) {
            view.setTitleText("Seong Eun Song");
            view.setSubTitleText("음악 정지");
            view.setPlayIcon();
            return;
        }

        if (session.isPaused()) {
            view.setTitleText("음악 일시 정지됨");
            view.setSubTitleText("재생 버튼을 누르면 이어서 재생됩니다.");
            view.setPlayIcon();
            return;
        }

        if (session.isError()) {
            view.setTitleText("오류 발생");
            view.setSubTitleText("재생 중 문제가 발생했습니다.");
            view.setPlayIcon();
            return;
        }

        if (session.isPlaying()) {
            Track track = session.currentTrack();
            if (track != null) {
                view.setTitleText("현재 재생 중인 파일은 " + track.displayName() + " 입니다.");
            } else {
                view.setTitleText("현재 재생 중");
            }
            view.setSubTitleText("음악 재생 중");
            view.setPauseIcon();
        }
    }
}
