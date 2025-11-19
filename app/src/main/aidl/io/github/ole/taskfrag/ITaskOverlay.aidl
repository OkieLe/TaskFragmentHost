package io.github.ole.taskfrag;

import io.github.ole.taskfrag.ITaskOverlayCallback;

interface ITaskOverlay {
    /**
     * Register an overlay to the host.
     * @param overlay overlay to register
     */
    oneway void registerOverlayCallback(ITaskOverlayCallback overlay);
    /**
     * Unregister an overlay from the host.
     * @param overlay overlay to unregister
     */
    oneway void unregisterOverlayCallback(ITaskOverlayCallback overlay);

    /**
     * Set whether the back event and scroll gesture should be intercepted by the host.
     */
    oneway void setInputInterceptable(boolean enabled);
}
