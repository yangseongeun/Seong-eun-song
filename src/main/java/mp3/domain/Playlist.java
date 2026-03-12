package mp3.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Playlist {

    private final List<Track> trackList = new ArrayList<>();
    private int index = 0;
    private RepeatMode mode = RepeatMode.NONE;

    public RepeatMode mode() {
        return mode;
    }

    public void cycleMode() {
        mode = mode.next();
    }

    public String modeLabel() {
        return mode.label();
    }

    public boolean isEmpty() {
        return trackList.isEmpty();
    }

    public int size() {
        return trackList.size();
    }

    public List<Track> trackListView() {
        return Collections.unmodifiableList(trackList);
    }

    public int index() {
        return index;
    }

    public void setIndex(int newIndex) {
        if (trackList.isEmpty()) {
            index = 0;
            return;
        }

        if (newIndex < 0) newIndex = 0;
        if (newIndex >= trackList.size()) newIndex = trackList.size() - 1;
        index = newIndex;
    }

    public Track currentOrNull() {
        if (trackList.isEmpty()) return null;

        if (index < 0 || index >= trackList.size()) {
            index = 0;
        }

        return trackList.get(index);
    }

    public boolean contains(File file) {
        return trackList.stream().anyMatch(track -> track.file().equals(file));
    }

    public void add(Track track) {
        if (track == null) return;

        trackList.add(track);
        if (trackList.size() == 1) {
            index = 0;
        }
    }

    public void addAll(List<Track> tracks) {
        if (tracks == null) return;
        for (Track track : tracks) {
            add(track);
        }
    }

    public void previous() {
        if (trackList.isEmpty()) return;
        index = (index - 1 + trackList.size()) % trackList.size();
    }

    public void nextManual() {
        if (trackList.isEmpty()) return;
        index++;
        if (index >= trackList.size()) index = 0;
    }

    public Track nextAuto() {
        if (trackList.isEmpty()) return null;

        return switch (mode) {
            case NONE -> {
                index++;
                if (index < trackList.size()) yield trackList.get(index);
                index = 0;
                yield null;
            }
            case ONE -> trackList.get(index);
            case ALL -> {
                index++;
                if (index >= trackList.size()) index = 0;
                yield trackList.get(index);
            }
        };
    }

    public void deleteIndices(int[] selectedIndices) {
        if (selectedIndices == null || selectedIndices.length == 0) return;

        for (int j = selectedIndices.length - 1; j >= 0; j--) {
            int removeIndex = selectedIndices[j];
            if (removeIndex < 0 || removeIndex >= trackList.size()) continue;

            trackList.remove(removeIndex);

            if (removeIndex <= index && index > 0) {
                index--;
            }
        }

        if (trackList.isEmpty()) index = 0;
        else if (index >= trackList.size()) index = trackList.size() - 1;
    }
}
