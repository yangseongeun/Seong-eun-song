package mp3.service;

import java.io.File;
import java.util.function.Consumer;

public interface PlaybackEngine {

    boolean isPlaying();

    void play(File file, Runnable onCompleted, Consumer<Exception> onError);

    void stop();

    void shutdown();
}
