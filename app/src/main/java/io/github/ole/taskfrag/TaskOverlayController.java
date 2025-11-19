package io.github.ole.taskfrag;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaskOverlayController {
    private static final String TAG = "TaskOverlayController";
    private final Context mContext;
    private final AtomicInteger mConnectionCount = new AtomicInteger(0);
    private Supplier<Boolean> mBackHandler;
    private Consumer<Integer> mScrollHandler;

    private boolean mBound;
    private ITaskOverlay mTaskOverlay;

    private final ITaskOverlayCallback mOverlayCallback = new ITaskOverlayCallback.Stub() {
        @Override
        public void onOverlayBackPressed() {
            if (mBackHandler != null) {
                Log.i(TAG, "Handled back: " + mBackHandler.get());
            }
        }

        @Override
        public void onOverlayScrolled(int scrollX) {
            if (mScrollHandler != null) {
                Log.i(TAG, "Handled scrollX: " + scrollX);
                mScrollHandler.accept(scrollX);
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
        mContext.unbindService(mConnection);
        mBound = false;
    }

    public void setBackHandler(Supplier<Boolean> handler) {
        mBackHandler = handler;
    }

    public void setScrollHandler(Consumer<Integer> handler) {
        mScrollHandler = handler;
    }

    public void setInputInterceptable(boolean enabled) {
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
