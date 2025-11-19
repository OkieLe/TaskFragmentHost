package io.github.ole.taskfrag;

interface ITaskOverlayCallback {

    /**
     * User scrolls the task overlay out
     * @param scrollX the x position of the scroll
     */
    oneway void onOverlayScrolled(int scrollX);
}
