package mp3.domain;

public record PlaybackSession(Track currentTrack, PlayerState state) {

    public static PlaybackSession stopped() {
        return new PlaybackSession(null, PlayerState.STOPPED);
    }

    public PlaybackSession withTrack(Track track) {
        return new PlaybackSession(track, state);
    }

    public PlaybackSession withState(PlayerState newState) {
        return new PlaybackSession(currentTrack, newState);
    }

    public boolean hasTrack() {
        return currentTrack != null;
    }

    public boolean isPlaying() {
        return state == PlayerState.PLAYING;
    }

    public boolean isPaused() {
        return state == PlayerState.PAUSED;
    }

    public boolean isStopped() {
        return state == PlayerState.STOPPED;
    }

    public boolean isError() {
        return state == PlayerState.ERROR;
    }
}