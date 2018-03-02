package tv.danmaku.ijk.media.widget;

import android.view.View;
import android.widget.MediaController;

/**
 * Created by lin on 2018/3/2.
 */

public interface IMediaController {
    void hide();

    boolean isShowing();

    void setAnchorView(View view);

    void setEnabled(boolean enabled);

    void setMediaPlayer(MediaController.MediaPlayerControl player);

    void show(int timeout);

    void show();

    //----------
    // Extends
    //----------
    void showOnce(View view);
}
