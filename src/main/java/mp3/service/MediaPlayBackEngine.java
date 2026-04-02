package mp3.service;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.function.Consumer;

/**
 * JavaFX MediaPlayer 기반 MP3 재생 엔진
 *
 * 주요 기능
 * 재생 / 일시정지 / 재개 / 정지
 * 현재 재생 시간 조회
 * 전체 길이 조회
 * seek(재생 위치 이동)
 * 볼륨 조절
 *
 * 개선 사항
 * 기존 단순 재생(JLayer) 구조에서 JavaFX MediaPlayer 기반으로 변경
 * 진행 바 / 시간 표시 / 볼륨 조절 기능 지원
 */
public class MediaPlayBackEngine {

    private MediaPlayer player;

    // ===== 상태(isPlaying, isPaused) =====
    public boolean isPlaying() {
        return player != null && player.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public boolean isPaused() {
        return player != null && player.getStatus() == MediaPlayer.Status.PAUSED;
    }

    // ===== 플레이백(play, pause, resume, stop, shutdown(종료 시)) =====
    public void play(File file, Runnable onCompleted, Consumer<Exception> onError) {
        stop();

        if (file == null) {
            return;
        }

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
            } catch (Exception ex) {
                if (onError != null) {
                    onError.accept(ex);
                }
            }
        });
    }

    public void pause() {
        if (player == null) {
            return;
        }

        Platform.runLater(() -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
            }
        });
    }

    public void resume() {
        if (player == null) {
            return;
        }

        Platform.runLater(() -> {
            if (player.getStatus() == MediaPlayer.Status.PAUSED) {
                player.play();
            }
        });
    }

    public void stop() {
        Platform.runLater(() -> {
            if (player == null) {
                return;
            }

            try {
                player.stop();
                player.dispose();
            } catch (Exception ignored) {
            } finally {
                player = null;
            }
        });
    }

    public void shutdown() {
        stop();
    }

    // ===== 시간(currentMillis, totalMillis, seekMillis) =====
    public long currentMillis() { // 현재 재생 위치(ms)
        if (player == null) {
            return 0L;
        }

        Duration current = player.getCurrentTime();
        return current == null ? 0L : (long) current.toMillis();
    }

    public long totalMillis() { // 전체 길이(ms)
        if (player == null) return 0L;
        Duration total = player.getTotalDuration();
        if (total == null || total.isUnknown()) return 0L;
        return (long) total.toMillis();
    }

    public void seekMillis(long millis) { // 재생 위치 이동
        if (player == null) return;

        Platform.runLater(() -> {
            Duration total = player.getTotalDuration();
            if (total == null || total.isUnknown()) return;

            double safe = Math.max(0, Math.min(millis, total.toMillis()));
            player.seek(Duration.millis(safe));
        });
    }

    // ===== Volume(음량)(setVolume, getVolume) =====
    public void setVolume(double volume) {
        if (player == null) {
            return;
        }

        Platform.runLater(() -> {
            double safe = Math.max(0.0, Math.min(1.0, volume));
            player.setVolume(safe);
        });
    }

    public double getVolume() {
        if (player == null) {
            return 0.5;
        }
        return player.getVolume();
    }
}