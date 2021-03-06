package com.doist.jobschedulercompat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.util.SparseArray;

import java.lang.ref.WeakReference;

/** @see android.app.job.JobService */
public abstract class JobService extends Service {
    private Binder binder;

    @NonNull
    @Override
    public final IBinder onBind(Intent intent) {
        if (binder == null) {
            binder = new Binder(this);
        }
        return binder;
    }

    /** @see android.app.job.JobService#onStartJob(android.app.job.JobParameters) */
    public abstract boolean onStartJob(JobParameters params);

    /** @see android.app.job.JobService#onStopJob(android.app.job.JobParameters) */
    public abstract boolean onStopJob(JobParameters params);

    /** @see android.app.job.JobService#jobFinished(android.app.job.JobParameters, boolean) */
    public final void jobFinished(JobParameters params, boolean needsReschedule) {
        if (binder != null) {
            binder.notifyJobFinished(params, needsReschedule);
        }
    }

    /**
     * Proxies callbacks from scheduler-specific job services to the user's {@link JobService}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class Binder extends android.os.Binder {
        private final WeakReference<JobService> serviceRef;
        private SparseArray<Callback> callbacks;

        Binder(JobService service) {
            super();
            this.serviceRef = new WeakReference<>(service);
            this.callbacks = new SparseArray<>(1);
        }

        public boolean startJob(JobParameters params, Callback callback) {
            JobService service = serviceRef.get();
            if (service != null) {
                callbacks.put(params.getJobId(), callback);
                return service.onStartJob(params);
            } else {
                return false;
            }
        }

        public boolean stopJob(JobParameters params) {
            JobService service = serviceRef.get();
            if (service != null) {
                callbacks.remove(params.getJobId());
                return service.onStopJob(params);
            } else {
                return false;
            }
        }

        void notifyJobFinished(JobParameters params, boolean needsReschedule) {
            Callback callback = callbacks.get(params.getJobId());
            if (callback != null) {
                callbacks.remove(params.getJobId());
                callback.jobFinished(params, needsReschedule);
            }
        }

        public interface Callback {
            void jobFinished(JobParameters params, boolean needsReschedule);
        }
    }
}
