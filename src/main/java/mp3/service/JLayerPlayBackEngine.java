package mp3.service;

import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class JLayerPlayBackEngine implements PlaybackEngine {

    private final ExecutorService playbackExec = newSingleThreadExecutor();
    private final Object pauseLock = new Object();

    private Future<?> playbackJob;

    private volatile Player currentPlayer;
    private volatile FileInputStream currentFileStream;
    private volatile BufferedInputStream currentBufferedStream;
    private volatile File currentFile;

    private volatile boolean playing = false; //재생 중인지 아닌지 판단하는 변수
    private volatile boolean paused = false; //일시정지 중인지 아닌지 판단하는 변수
    private volatile boolean stopRequested = false;

    public boolean isPlaying() {
        return playing;
    }

    public boolean isPaused() {
        return paused;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public boolean isCurrentFile(File file) {
        return file != null && file.equals(currentFile);
    }

    /*
     * 비동기 재생 시작.
     * onCompleted / onError는 "호출 스레드가 무엇인지" 보장하지 않음.
     * (즉, UI 업데이트는 호출 측에서 SwingUtilities/Platform.runLater로 감싸야 함)
     */

    public void play(File file, Runnable onCompleted, Consumer<Exception> onError) {
        if (file == null) return;

        stop();

        currentFile = file;
        stopRequested = false;
        paused = false;

        playbackJob = playbackExec.submit(() -> {
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                currentFileStream = fis;
                currentBufferedStream = bis;
                currentPlayer = new Player(bis);

                playing = true;

                // 1프레임씩 재생하면서 pause 제어
                while (!stopRequested) {
                    synchronized (pauseLock) {
                        while (paused && !stopRequested) {
                            pauseLock.wait();
                        }
                    }

                    if (stopRequested) break;

                    boolean hasMore = currentPlayer.play(1);
                    if (!hasMore) {
                        break; // EOF
                    }
                }

                if (!stopRequested && onCompleted != null) {
                    onCompleted.run();
                }

            } catch (Exception ex) {
                if (!stopRequested && onError != null) {
                    onError.accept(ex);
                }
            } finally {
                playing = false;
                paused = false;
                closeResources();
                currentFile = null;
            }
        });
    }

    public void pause() {
        if (!playing || paused) return;
        paused = true;
    }

    public void resume() {
        if (!playing || !paused) return;

        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    /** 안전 정지(협력적 취소) */
    public void stop() {
        stopRequested = true;
        paused = false;

        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        try {
            if (currentPlayer != null) currentPlayer.close();
        } catch (Exception ignored) {}

        if (playbackJob != null) {
            playbackJob.cancel(true);
            playbackJob = null;
        }

        closeResources();
        playing = false;
        paused = false;
        currentFile = null;
    }

    public void shutdown() {
        stop();
        playbackExec.shutdownNow();
    }

    private void closeResources() {
        try {
            if (currentPlayer != null) currentPlayer.close();
        } catch (Exception ignored) {}
        try {
            if (currentBufferedStream != null) currentBufferedStream.close();
        } catch (Exception ignored) {}
        try {
            if (currentFileStream != null) currentFileStream.close();
        } catch (Exception ignored) {}

        currentPlayer = null;
        currentBufferedStream = null;
        currentFileStream = null;
    }
}
