package io.github.ole.taskfrag.shared;

interface ITaskOverlayCallback {

    /**
     * User scrolls the task overlay out
     * @param scrollX the x position of the scroll
     * @param scrolling whether the scrolling is still going on
     */
    oneway void onOverlayScrolled(int scrollX, boolean scrolling);
}
