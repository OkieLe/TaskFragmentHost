package io.github.ole.taskfrag;

interface ITaskHostCallback {

    /**
     * Set input interceptable in task
     * @param enabled send back event and scroll gesture in task to host or not
     */
    oneway void setInputInterceptable(boolean enabled);
}
