package mp3.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Playlist {
    private final List<File> fileList = new ArrayList<>();
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
        return fileList.isEmpty();
    }

    public int size() {
        return fileList.size();
    }

    public List<File> fileListView() {
        return Collections.unmodifiableList(fileList);
    }

    public int index() {
        return index;
    }

    public void setIndex(int newIndex) {
        if (fileList.isEmpty()) {
            index = 0;
            return;
        }
        if (newIndex < 0) newIndex = 0;
        if (newIndex >= fileList.size()) newIndex = fileList.size() - 1;
        index = newIndex;
    }

    public File currentOrNull() {
        if (fileList.isEmpty()) return null;

        // 만약 아무것도 선택되지 않는다면 범위 내의 인덱스 유지
        if (index < 0 || index >= fileList.size()) index = 0;
        return fileList.get(index);
    }

    public boolean contains(File f) {
        return fileList.contains(f);
    }

    public void add(File f) {
        if (f == null) return;
        fileList.add(f);
        if (fileList.size() == 1) index = 0;
    }

    public void addAll(List<File> list) {
        if (list == null) return;
        for (File f : list) add(f);
    }

    public void previous() {
        if (fileList.isEmpty()) return;
        index = (index - 1 + fileList.size()) % fileList.size(); //인덱스 감소 - 이전 트랙 음악 선택, 0일 때의 경우 생각
    }

    public void nextManual() {
        if (fileList.isEmpty()) return;
        index++;
        if (index >= fileList.size()) index = 0;
    }

    /**
     * 트랙이 "자연 종료"되었을 때 다음 인덱스를 결정한다.
     * @return 다음에 재생할 파일. 더 이상 재생할 게 없으면 null.
     */
    public File nextAuto() {
        if (fileList.isEmpty()) return null;

        return switch (mode) {
            case NONE -> {
                index++;
                if (index < fileList.size()) yield fileList.get(index);

                // 끝까지 재생 완료
                index = 0;
                yield null;
            }
            case ONE -> fileList.get(index);
            case ALL -> {
                index++;
                if (index >= fileList.size()) index = 0;
                yield fileList.get(index);
            }
        };
    }

    /**
     * JList에서 선택된 인덱스들을 삭제하고 현재 index를 보정한다.
     * @return 실제로 삭제된 파일 이름 목록(필요시 사용)
     */
    public void deleteIndices(int[] selectedIndices) {
        if (selectedIndices == null || selectedIndices.length == 0) return;

        // 역순 삭제(인덱스 깨짐 방지)
        for (int j = selectedIndices.length - 1; j >= 0; j--) {
            int removeIndex = selectedIndices[j];
            if (removeIndex < 0 || removeIndex >= fileList.size()) continue;

            fileList.remove(removeIndex); //인덱스 j의 파일을 지운다. 없으면 파일은 남아있는데 이름만 지워지고 인덱스가 재설정이 안된다.

            // 현재 index 보정(네 기존 로직 유지)
            if (removeIndex <= index && index > 0) {
                index--;
            }
        }

        // 최종 보정
        if (fileList.isEmpty()) index = 0;
        else if (index >= fileList.size()) index = fileList.size() - 1;
    }

}
