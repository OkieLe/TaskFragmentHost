package io.github.ole.taskfrag;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

public class TaskChannelService extends Service {
    private static final String TAG = "TaskChannelService";
    private static final String ACTION_HOST = "io.github.ole.taskfrag.action.GET_HOST";
    private static final String ACTION_OVERLAY = "io.github.ole.taskfrag.action.GET_OVERLAY";

    private boolean mInputInterceptable = false;
    private ITaskOverlayCallback mTaskOverlayCallback;
    private final IBinder.DeathRecipient mOverlayRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mTaskOverlayCallback = null;
        }
    };
    private final ITaskOverlay mTaskOverlay = new ITaskOverlay.Stub() {
        @Override
        public void registerOverlayCallback(ITaskOverlayCallback overlay) {
            mTaskOverlayCallback = overlay;
            if (mTaskOverlayCallback != null) {
                try {
                    mTaskOverlayCallback.asBinder().linkToDeath(mOverlayRecipient, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to link to death", e);
                }
            }
        }

        @Override
        public void unregisterOverlayCallback(ITaskOverlayCallback overlay) {
            if (mTaskOverlayCallback == null) {
                return;
            }
            mTaskOverlayCallback.asBinder().unlinkToDeath(mOverlayRecipient, 0);
            mTaskOverlayCallback = null;
        }

        @Override
        public void setInputInterceptable(boolean enabled) {
            mInputInterceptable = enabled;
            if (mTaskHostCallback != null) {
                try {
                    mTaskHostCallback.setInputInterceptable(enabled);
                } catch (RemoteException e) {
                    Log.i(TAG, "Failed to set back interceptable");
                }
            }
        }
    };
    private ITaskHostCallback mTaskHostCallback;
    private final IBinder.DeathRecipient mTaskHostRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mTaskHostCallback = null;
        }
    };
    private final ITaskHost mTaskHost = new ITaskHost.Stub() {
        @Override
        public void registerHostCallback(ITaskHostCallback host) {
            mTaskHostCallback = host;
            if (mTaskHostCallback != null) {
                try {
                    mTaskHostCallback.asBinder().linkToDeath(mTaskHostRecipient, 0);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to link to death", e);
                }
                try {
                    mTaskHostCallback.setInputInterceptable(mInputInterceptable);
                } catch (RemoteException e) {
                    Log.i(TAG, "Failed to set back interceptable");
                }
            }
        }

        @Override
        public void unregisterHostCallback(ITaskHostCallback host) {
            if (mTaskHostCallback == null) {
                return;
            }
            mTaskHostCallback.asBinder().unlinkToDeath(mTaskHostRecipient, 0);
            mTaskHostCallback = null;
        }

        @Override
        public void onOverlayBackPressed() {
            if (mTaskOverlayCallback != null) {
                try {
                    mTaskOverlayCallback.onOverlayBackPressed();
                } catch (RemoteException e) {
                    throw new IllegalStateException("Task host failed to handle back");
                }
            } else {
                throw new IllegalStateException("No task host to handle back");
            }
        }

        @Override
        public void onOverlayScrolled(int scrollX) {
            if (mTaskOverlayCallback != null) {
                try {
                    mTaskOverlayCallback.onOverlayScrolled(scrollX);
                } catch (RemoteException e) {
                    throw new IllegalStateException("Task host failed to handle scroll");
                }
            } else {
                throw new IllegalStateException("No task host to handle scroll");
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION_HOST.equals(intent.getAction())) {
            return mTaskHost.asBinder();
        } else if (ACTION_OVERLAY.equals(intent.getAction())) {
            return mTaskOverlay.asBinder();
        }
        return null;
    }
}
