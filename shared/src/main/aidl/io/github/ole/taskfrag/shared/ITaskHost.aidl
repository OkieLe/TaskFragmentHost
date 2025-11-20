package io.github.ole.taskfrag.shared;

import io.github.ole.taskfrag.shared.ITaskHostCallback;

interface ITaskHost {

    /**
     * Register a host to the channel
     * @param host host to register
     */
    oneway void registerHostCallback(ITaskHostCallback host);

    /**
     * Unregister a host from the channel
     * @param host host to unregister
     */
    oneway void unregisterHostCallback(ITaskHostCallback host);

    /**
     * User scrolls the task overlay out
     * @param scrollX the x position of the scroll
     * @param scrolling whether the scrolling is still going on
     */
    oneway void onOverlayScrolled(int scrollX, boolean scrolling);
}
