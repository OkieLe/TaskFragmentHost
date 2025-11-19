package io.github.ole.taskfrag;

interface ITaskOverlayCallback {

    /**
     * User presses the back button in overlay, this should be handled by host
     */
    oneway void onOverlayBackPressed();

    /**
     * User scrolls the task overlay out
     * @param scrollX the x position of the scroll
     */
    oneway void onOverlayScrolled(int scrollX);
}
