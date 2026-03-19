package mp3.controller;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mp3.domain.PlaybackSession;
import mp3.domain.PlayerState;
import mp3.domain.Playlist;
import mp3.domain.Track;
import mp3.service.MediaPlayBackEngine;
import mp3.ui.MP3PlayerFrame;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class PlayerController implements ActionListener, MouseListener, KeyListener {

    private final MP3PlayerFrame playerFrame;
    private final Playlist playlist;
    private final MediaPlayBackEngine playBackEngine;

    private PlaybackSession session = PlaybackSession.stopped();

    // [추가] 진행 상황 UI 갱신용 타이머
    private final Timer progressTimer;

    // [추가] 사용자가 슬라이더를 직접 움직이는 중인지
    private boolean userSeeking = false;

    public PlayerController(MP3PlayerFrame playerFrame, Playlist playlist, MediaPlayBackEngine playBackEngine) {
        this.playerFrame = playerFrame;
        this.playlist = playlist;
        this.playBackEngine = playBackEngine;

        // 300ms마다 진행 바 / 시간 갱신
        this.progressTimer = new Timer(300, e -> refreshProgressUi());

        bindExtraUiEvents();
        playerFrame.setVolumeValue(50);
    }

    private void bindExtraUiEvents() {
        // [추가] 진행 바 드래그 후 seek
        playerFrame.progressSlider().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                userSeeking = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                userSeeking = false;
                long total = playBackEngine.totalMillis();
                if (total <= 0) return;

                int sliderMax = playerFrame.progressSlider().getMaximum();
                int sliderValue = playerFrame.getProgressValue();

                long target = (long) ((sliderValue / (double) sliderMax) * total);
                playBackEngine.seekMillis(target);
                refreshProgressUi();
            }
        });

        // [추가] 볼륨 조절
        playerFrame.volumeSlider().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double volume = playerFrame.getVolumeValue() / 100.0;
                playBackEngine.setVolume(volume);
            }
        });
    }

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
        if (e.getComponent() instanceof JList && e.getClickCount() == 1) {
            int clicked = playerFrame.listIndexAt(e.getPoint());
            playlist.setIndex(clicked);
            return;
        }

        if (e.getComponent() instanceof JList
                && e.getClickCount() == 2
                && e.getButton() == MouseEvent.BUTTON1) {
            int clicked = playerFrame.listIndexAt(e.getPoint());
            if (clicked < 0) return;
            playlist.setIndex(clicked);
            playSelected();
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public void openFiles() {
        Platform.runLater(() -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("mp3 파일 선택하기");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("음악 파일(*.mp3)", "*.mp3")
            );

            List<File> selected = chooser.showOpenMultipleDialog(new Stage());
            if (selected == null || selected.isEmpty()) return;

            SwingUtilities.invokeLater(() -> {
                DefaultListModel<String> model = playerFrame.listModel();

                for (File f : selected) {
                    if (f == null) continue;

                    if (playlist.contains(f)) {
                        JOptionPane.showMessageDialog(
                                null, "이미 존재하는 파일입니다.",
                                "알림", JOptionPane.INFORMATION_MESSAGE
                        );
                        continue;
                    }

                    if (!f.getName().toLowerCase().endsWith("mp3")) {
                        JOptionPane.showMessageDialog(
                                null,
                                "파일 형식이 올바르지 않습니다.\n이 플레이어는 확장자가 mp3인 파일만 지원합니다.",
                                "경고",
                                JOptionPane.WARNING_MESSAGE
                        );
                        continue;
                    }

                    Track track = Track.from(f);
                    playlist.add(track);
                    model.addElement(track.displayName());

                    if (!playBackEngine.isPlaying()) {
                        playerFrame.setSubTitleText("음악 파일을 재생하려면 재생 버튼을 누르세요.");
                    }
                }
            });
        });
    }

    public void exitApp() {
        if (playerFrame.confirmExit()) {
            progressTimer.stop();
            playBackEngine.shutdown();
            System.exit(0);
        }
    }

    public void showInfo() {
        playerFrame.showInfoDialog();
    }

    public void prev() {
        if (playlist.isEmpty()) {
            playerFrame.showNoFileError();
            return;
        }

        playlist.previous();
        playBackEngine.stop();
        session = session.withState(PlayerState.STOPPED);
        startCurrent();
    }

    public void nextManual() {
        if (playlist.isEmpty()) {
            playerFrame.showNoFileError();
            return;
        }

        playlist.nextManual();
        playBackEngine.stop();
        session = session.withState(PlayerState.STOPPED);
        startCurrent();
    }

    public void togglePlayPause() {
        if (playlist.isEmpty()) {
            playerFrame.showNoFileError();
            return;
        }

        if (playBackEngine.isPlaying() && !playBackEngine.isPaused()) {
            playBackEngine.pause();
            session = session.withState(PlayerState.PAUSED);
            renderSession();
            playerFrame.setTitleText("음악 일시 정지됨");
            playerFrame.setSubTitleText("재생 버튼을 누르면 이어서 재생됩니다.");
            playerFrame.setPlayIcon();

            progressTimer.stop();

            return;
        }

        if (playBackEngine.isPaused()) {
            playBackEngine.resume();

            Track track = playlist.currentOrNull();
            if (track != null) {
                playerFrame.setTitleText("현재 재생 중인 파일은 " + track.displayName() + " 입니다.");
            }
            playerFrame.setSubTitleText("음악 재생 중");
            playerFrame.setPauseIcon();

            // 재개 시 다시 진행 UI 갱신 시작
            progressTimer.start();
            return;
        }

        playSelectedOrCurrent();
    }

    public void stop() {
        if (playlist.isEmpty()) {
            playerFrame.showNoFileError();
            return;
        }

        playBackEngine.stop();
        progressTimer.stop();

        session = session.withState(PlayerState.STOPPED);
        renderSession();

        playerFrame.setTitleText("Seong Eun Song");
        playerFrame.setSubTitleText("음악 정지");
        playerFrame.setPlayIcon();

        resetProgressUi();
    }

    private void playSelectedOrCurrent() {
        if (playlist.isEmpty()) {
            playerFrame.showNoFileError();
            return;
        }
        startCurrent();
    }

    private void playSelected() {
        if (playlist.isEmpty()) return;

        playBackEngine.stop();
        session = session.withState(PlayerState.STOPPED);
        startCurrent();
    }

    private void startCurrent() {
        Track track = playlist.currentOrNull();
        if (track == null) {
            playerFrame.showNoFileError();
            session = PlaybackSession.stopped();
            return;
        }

        playerFrame.setSelectedIndex(playlist.currentIndex());

        // 새 곡 시작 전에 진행 UI는 0으로 보정
        resetProgressUi();

        session = new PlaybackSession(track, PlayerState.PLAYING);
        renderSession();

        playBackEngine.play(
                track.file(),
                () -> SwingUtilities.invokeLater(this::onTrackCompleted),
                ex -> SwingUtilities.invokeLater(() -> {
                    progressTimer.stop();
                    session = session.withState(PlayerState.ERROR);
                    playerFrame.showError("재생 오류: " + ex.getMessage());
                    renderSession();

                    // 오류 시에는 초기화
                    resetProgressUi();
                })
        );

        progressTimer.start();
    }

    private void onTrackCompleted() {
        if (playlist.isEmpty()) {
            progressTimer.stop();
            session = PlaybackSession.stopped();
            resetToIdle("Seong Eun Song", "재생할 파일이 없습니다.");
            resetProgressUi();
            return;
        }

        // 다음 곡이 없으면 완전 종료 상태 -> 00:00 초기화
        Track next = playlist.nextAuto();
        if (next == null) {
            playBackEngine.stop();
            progressTimer.stop();
            session = session.withState(PlayerState.STOPPED);
            resetToIdle("Seong Eun Song", "리스트에 있는 모든 음악 재생 끝");
            resetProgressUi();
            return;
        }

        // 다음 곡이 있으면 초기화 후 바로 다음 곡 시작
        startCurrent();
    }

    public void cycleRepeat() {
        playlist.cycleMode();
        playerFrame.setRepeatText(playlist.modeLabel());
    }

    private void deleteSelected() {
        int[] selected = playerFrame.selectedIndices();
        if (selected == null || selected.length == 0) return;

        Track current = playlist.currentOrNull();
        boolean deletingCurrent = false;

        if (current != null) {
            for (int idx : selected) {
                Track target = playlist.trackAt(idx);
                if (target != null && target.equals(current)) {
                    deletingCurrent = true;
                    break;
                }
            }
        }

        if (deletingCurrent) {
            playBackEngine.stop();
            progressTimer.stop();
            session = session.withState(PlayerState.STOPPED);
        }

        DefaultListModel<String> model = playerFrame.listModel();
        playlist.deleteIndices(selected);

        for (int i = selected.length - 1; i >= 0; i--) {
            int idx = selected[i];
            if (idx >= 0 && idx < model.size()) {
                model.remove(idx);
            }
        }

        if (playlist.isEmpty()) {
            resetToIdle("Seong Eun Song", "재생할 파일이 없습니다.");
            resetProgressUi();
        } else if (deletingCurrent) {
            startCurrent();
        } else {
            playerFrame.setSelectedIndex(playlist.currentIndex());
        }

        playerFrame.repaintPlaylist();
    }

    private void resetToIdle(String title, String subtitle) {
        session = session.withState(PlayerState.STOPPED);
        playerFrame.setTitleText(title);
        playerFrame.setSubTitleText(subtitle);
        playerFrame.setPlayIcon();
    }

    private void renderSession() {
        if (session.isStopped()) {
            playerFrame.setTitleText("Seong Eun Song");
            playerFrame.setSubTitleText("음악 정지");
            playerFrame.setPlayIcon();
            return;
        }

        if (session.isPaused()) {
            playerFrame.setTitleText("음악 일시 정지됨");
            playerFrame.setSubTitleText("재생 버튼을 누르면 이어서 재생됩니다.");
            playerFrame.setPlayIcon();
            return;
        }

        if (session.isError()) {
            playerFrame.setTitleText("오류 발생");
            playerFrame.setSubTitleText("재생 중 문제가 발생했습니다.");
            playerFrame.setPlayIcon();
            return;
        }

        if (session.isPlaying()) {
            Track track = session.currentTrack();
            if (track != null) {
                playerFrame.setTitleText("현재 재생 중인 파일은 " + track.displayName() + " 입니다.");
            } else {
                playerFrame.setTitleText("현재 재생 중");
            }

            playerFrame.setSubTitleText("음악 재생 중");
            playerFrame.setPauseIcon();
        }
    }

    // ===== [추가] 진행 바/시간 표시 =====

    private void refreshProgressUi() {
        if (userSeeking) return;

        long current = playBackEngine.currentMillis();
        long total = playBackEngine.totalMillis();

        if (total <= 0) {
            playerFrame.setProgressValue(0);
            playerFrame.setCurrentTimeText("00:00");
            playerFrame.setTotalTimeText("00:00");
            return;
        }

        int sliderMax = playerFrame.progressSlider().getMaximum();
        int value = (int) ((current / (double) total) * sliderMax);

        playerFrame.setProgressValue(value);
        playerFrame.setCurrentTimeText(formatMillis(current));
        playerFrame.setTotalTimeText(formatMillis(total));
    }

    private void resetProgressUi() {
        playerFrame.setProgressValue(0);
        playerFrame.setCurrentTimeText("00:00");
        playerFrame.setTotalTimeText("00:00");
    }

    private String formatMillis(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}