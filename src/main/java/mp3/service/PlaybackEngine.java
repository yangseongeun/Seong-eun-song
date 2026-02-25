package mp3.service;
import java.io.File;

public interface PlaybackEngine {
    void play(File file) throws Exception; // blocking play
    void stop();                           // request stop
}
