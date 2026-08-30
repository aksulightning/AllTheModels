package com.aksulightning.platform.fabric;

import com.aksulightning.platform.PlatformEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class FabricPlatformEvents implements PlatformEvents {
    @Override
    public void registerClientStarted(Runnable callback) {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> callback.run());
    }
}
