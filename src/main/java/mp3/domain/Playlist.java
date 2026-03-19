package mp3.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * [기능]
 * - 재생 목록(Track 목록) 관리
 * - 현재 선택/재생 인덱스 관리
 * - 반복 모드 관리
 *
 * [수정 포인트]
 * 1. currentIndex() 추가
 *    -> Controller에서 현재 재생 곡과 JList 선택 상태를 동기화할 수 있게 함
 *
 * 2. trackAt(int index) 추가
 *    -> 삭제 대상이 현재 재생 중인 곡인지 판별할 수 있게 함
 */

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

    /**
     * [추가 기능]
     * 현재 재생/선택 인덱스를 외부에서 명확히 조회
     */
    public int currentIndex() {
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

    /**
     * [추가 기능]
     * 인덱스로 Track 조회
     * -> 삭제 대상과 현재 곡 비교용
     */
    public Track trackAt(int index) {
        if (index < 0 || index >= trackList.size()) return null;
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

    /**
     * [기능]
     * 선택된 인덱스들을 삭제
     *
     * [유지된 동작]
     * - 뒤에서부터 삭제하여 인덱스 밀림 방지
     * - 현재 index 보정
     */
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
