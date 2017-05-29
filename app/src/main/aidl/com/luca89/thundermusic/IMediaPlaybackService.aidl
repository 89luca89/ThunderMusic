package thundermusic;

import android.graphics.Bitmap;

interface IMediaPlaybackService
{
    void openOnline(String url);
    void setOnlineLibrary(String name, int position);
    int getQueuePosition();
    int getBufferState();
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    void cycleRepeat();
    void toggleShuffle();
    long duration();
    long position();
    long seek(long pos);
    long[] getQueue();
    String getTrackName();
    String getAlbumName();
    String getTrackTrack();
    String getTrackLink();
    int getDuration();
    long getAlbumId();
    String getArtistName();
    void setQueuePosition(int index);
    String getPath();
    String getAudioUrl();
    long getAudioId();
    void setShuffleMode(int shufflemode);
    int getShuffleMode();
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
    void startPopup();
    void closePopup();
    void notifyChange(String what);
    void exit();
    void dismissAllNotifications();
}

