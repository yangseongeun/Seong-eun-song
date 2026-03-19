package mp3.domain;

public enum RepeatMode {
    NONE("반복 없음"), //무반복 모드
    ONE("한곡 반복"), //한번 반복 모드
    ALL("전체 반복"); //전체 반복 모드

    private final String label;

    RepeatMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public RepeatMode next() {
        return switch (this) {
            case NONE -> ONE;
            case ONE -> ALL;
            case ALL -> NONE;
        };
    }
}