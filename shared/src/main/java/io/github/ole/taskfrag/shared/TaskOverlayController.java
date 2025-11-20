package io.github.ole.taskfrag.shared;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

public class TaskOverlayController {
    public interface InputHandler {
        /**
         * Called when the overlay receives back
         * @return true if back is handled by host, false if the original window should handle it
         */
        boolean onBackPressed();
        /**
         * Called when the overlay has scrolled.
         *
         * @param scrollX The current scrollX of the overlay.
         * @param scrolling Whether the overlay is currently scrolling.
         */
        void onScrolled(int scrollX, boolean scrolling);
    }
    private static final String TAG = "TaskOverlayController";
    private final Context mContext;
    private final AtomicInteger mConnectionCount = new AtomicInteger(0);
    private InputHandler mInputHandler;

    private boolean mBound;
    private boolean mInputInterceptable = false;
    private ITaskOverlay mTaskOverlay;

    private final ITaskOverlayCallback mOverlayCallback = new ITaskOverlayCallback.Stub() {
        @Override
        public boolean onOverlayBackPressed() {
            return mInputHandler != null && mInputHandler.onBackPressed();
        }

        @Override
        public void onOverlayScrolled(int scrollX, boolean scrolling) {
            if (mInputHandler != null) {
                mInputHandler.onScrolled(scrollX, scrolling);
            }
        }
    };

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mTaskOverlay = null;
        }
    };
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTaskOverlay = ITaskOverlay.Stub.asInterface(service);
            if (mTaskOverlay == null) {
                return;
            }
            Log.d(TAG, "ITaskOverlay connected");
            try {
                mTaskOverlay.asBinder().linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to link to death", e);
                return;
            }
            try {
                mTaskOverlay.registerOverlayCallback(mOverlayCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to register overlay", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (mTaskOverlay == null) {
                return;
            }
            Log.d(TAG, "ITaskOverlay disconnected");
            try {
                mTaskOverlay.unregisterOverlayCallback(mOverlayCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to unregister overlay", e);
            }
            mTaskOverlay.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mTaskOverlay = null;
        }
    };

    private static volatile TaskOverlayController sInstance;
    public static TaskOverlayController get(Context context) {
        if (sInstance == null) {
            synchronized (TaskOverlayController.class) {
                if (sInstance == null) {
                    sInstance = new TaskOverlayController(context);
                }
            }
        }
        return sInstance;
    }

    private TaskOverlayController(Context context) {
        mContext = context.getApplicationContext();
    }

    public void start() {
        mConnectionCount.incrementAndGet();
        if (mTaskOverlay != null) {
            return;
        }
        if (mBound) {
            unbindTaskOverlay();
        }
        bindTaskOverlay();
    }

    private void bindTaskOverlay() {
        Intent intent = new Intent("io.github.ole.taskfrag.action.GET_OVERLAY");
        intent.setPackage("io.github.ole.taskfrag");
        mBound = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindTaskOverlay() {
        if (!mBound) {
            return;
        }
        mContext.unbindService(mConnection);
        mBound = false;
    }

    public void setInputHandler(InputHandler handler) {
        mInputHandler = handler;
    }

    public void setInputInterceptable(boolean enabled) {
        if (mInputInterceptable == enabled) {
            return;
        }
        mInputInterceptable = enabled;
        if (mTaskOverlay == null) {
            return;
        }
        try {
            mTaskOverlay.setInputInterceptable(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to set back interceptable", e);
        }
    }

    public void stop() {
        int count = mConnectionCount.decrementAndGet();
        if (mTaskOverlay == null || count > 0) {
            return;
        }
        unbindTaskOverlay();
    }
}
