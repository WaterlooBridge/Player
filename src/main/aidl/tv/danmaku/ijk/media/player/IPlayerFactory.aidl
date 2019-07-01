// IPlayerFactory.aidl
package tv.danmaku.ijk.media.player;

import tv.danmaku.ijk.media.player.IIjkMediaPlayer;

// Declare any non-default types here with import statements

interface IPlayerFactory {

    IIjkMediaPlayer createPlayer();
}
