package mp3.service;

import javazoom.jl.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class JLayerPlayBackEngine {

    private final ExecutorService playbackExec = newSingleThreadExecutor();
    private Future<?> playbackJob;

    private volatile Player currentPlayer;
    private volatile FileInputStream currentStream;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    // 완료 이벤트 꼬임 방지 토큰
    private final AtomicLong playIdGen = new AtomicLong(0);
    private volatile long activePlayId = 0;

    private volatile boolean playing = false; //재생 중인지 아닌지 판단하는 변수

    public boolean isPlaying() {
        return playing;
    }

    /*
     * 비동기 재생 시작.
     * onCompleted / onError는 "호출 스레드가 무엇인지" 보장하지 않음.
     * (즉, UI 업데이트는 호출 측에서 SwingUtilities/Platform.runLater로 감싸야 함)
     */

    public void play(File file, Runnable onCompleted, java.util.function.Consumer<Exception> onError) {
        if (file == null) return;

        stop(); // 기존 재생 안전 종료

        final long playId = playIdGen.incrementAndGet();
        activePlayId = playId;

        playbackJob = playbackExec.submit(() -> {
            stopRequested.set(false);

            try (FileInputStream fis = new FileInputStream(file)) {
                currentStream = fis;
                currentPlayer = new Player(fis);
                playing = true;

                // Blocking play (background thread)
                currentPlayer.play();

                // 완료 콜백(이전 작업이면 무시)
                if (playId == activePlayId && !stopRequested.get() && !Thread.currentThread().isInterrupted()) {
                    if (onCompleted != null) onCompleted.run();
                }
            } catch (Exception ex) {
                // stop/cancel로 인한 예외면 조용히 종료
                if (stopRequested.get() || Thread.currentThread().isInterrupted()) return;

                if (onError != null) onError.accept(ex);
            } finally {
                playing = false;
                currentPlayer = null;
                currentStream = null;
            }
        });
    }

    public void shutdown() {
        stop();
        playbackExec.shutdownNow();
    }

    /** 안전 정지(협력적 취소) */
    public void stop() {
        stopRequested.set(true);

        // 기존 작업 무효화(완료 이벤트가 늦게 와도 무시)
        activePlayId = playIdGen.incrementAndGet();
        // ===== 기존 스레드 취소 =====
        if (playbackJob != null) {
            playbackJob.cancel(true);
            playbackJob = null;
        }

        // ===== 스트림 닫아서 재생 종료 =====
        try {
            if (currentStream != null) currentStream.close();
        } catch (Exception ignored) {}

        currentPlayer = null;
        currentStream = null;
        playing = false; //파워 꺼짐
    }
}
