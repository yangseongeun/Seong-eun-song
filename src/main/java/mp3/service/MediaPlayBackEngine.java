package mp3.service;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.function.Consumer;

/**
 * [기능]
 * - JavaFX MediaPlayer 기반 MP3 재생 엔진
 * - 재생 / 일시정지 / 재개 / 정지
 * - 진행 시간 조회
 * - 전체 길이 조회
 * - seek(이동)
 * - volume 조절
 *
 * [수정 강조]
 * - 기존 JLayer 기반 엔진 대신 MediaPlayer 기반으로 변경
 * - 진행 바 / 시간 표시 / 볼륨 조절 기능 지원
 */
public class MediaPlayBackEngine {

    private MediaPlayer player;
    private volatile boolean paused = false;

    public boolean isPlaying() {
        return player != null && player.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public boolean isPaused() {
        return player != null && player.getStatus() == MediaPlayer.Status.PAUSED;
    }

    public void play(File file, Runnable onCompleted, Consumer<Exception> onError) {
        stop();

        if (file == null) return;

        Platform.runLater(() -> {
            try {
                Media media = new Media(file.toURI().toString());
                player = new MediaPlayer(media);

                player.setOnEndOfMedia(() -> {
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                });

                player.setOnError(() -> {
                    if (onError != null && player.getError() != null) {
                        onError.accept(player.getError());
                    }
                });

                player.play();
                paused = false;

            } catch (Exception ex) {
                if (onError != null) {
                    onError.accept(ex);
                }
            }
        });
    }

    public void pause() {
        if (player == null) return;

        Platform.runLater(() -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                paused = true;
            }
        });
    }

    public void resume() {
        if (player == null) return;

        Platform.runLater(() -> {
            if (player.getStatus() == MediaPlayer.Status.PAUSED) {
                player.play();
                paused = false;
            }
        });
    }

    public void stop() {
        Platform.runLater(() -> {
            if (player != null) {
                try {
                    player.stop();
                    player.dispose();
                } catch (Exception ignored) {
                } finally {
                    player = null;
                    paused = false;
                }
            }
        });
    }

    public void shutdown() {
        stop();
    }

    /**
     * [추가 기능]
     * 현재 재생 위치(ms)
     */
    public long currentMillis() {
        if (player == null) return 0L;
        Duration d = player.getCurrentTime();
        return d == null ? 0L : (long) d.toMillis();
    }

    /**
     * [추가 기능]
     * 전체 길이(ms)
     */
    public long totalMillis() {
        if (player == null) return 0L;
        Duration d = player.getTotalDuration();
        if (d == null || d.isUnknown()) return 0L;
        return (long) d.toMillis();
    }

    /**
     * [추가 기능]
     * 재생 위치 이동
     */
    public void seekMillis(long millis) {
        if (player == null) return;

        Platform.runLater(() -> {
            Duration total = player.getTotalDuration();
            if (total == null || total.isUnknown()) return;

            double safe = Math.max(0, Math.min(millis, total.toMillis()));
            player.seek(Duration.millis(safe));
        });
    }

    /**
     * [추가 기능]
     * 0.0 ~ 1.0
     */
    public void setVolume(double volume) {
        if (player == null) return;

        Platform.runLater(() -> {
            double safe = Math.max(0.0, Math.min(1.0, volume));
            player.setVolume(safe);
        });
    }

    public double getVolume() {
        if (player == null) return 0.5;
        return player.getVolume();
    }
}