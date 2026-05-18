package com.aksulightning.platform;

public interface PlatformEvents {
    void registerClientStarted(Runnable callback);

    void registerClientJoined(Runnable callback);

    void registerClientDisconnected(Runnable callback);
}
