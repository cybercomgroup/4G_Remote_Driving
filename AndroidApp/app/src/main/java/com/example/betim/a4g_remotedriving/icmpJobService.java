package com.example.betim.a4g_remotedriving;

import android.app.Activity;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static android.R.attr.host;

/**
 * Created by Victor on 2017-02-18.
 */

public class icmpJobService  extends JobService{
    private static final String TAG = icmpJobService.class.getName();
    private UpdatePingAsyncTask updatePing = new UpdatePingAsyncTask();
    private int pingresult;

    public icmpJobService(){
        pingresult = 666;
    }

    /**
     * Method to offload asyncronous requests from applications main Thread.
     * @param params
     * @return
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.wtf(TAG, "onStartJob id=" + params.getJobId());
        updatePing.execute(params);
        return true;
    }


    /**
     * Called when requirements specified at schedule time are no longer met.
     * setPeriodic(long intervalMillis) would repeat the JobService within given interval but
     * executes this method if the JobService isn't executed within the time interval during
     * onStartJob() method and reschedules it.
     *
     * @param params
     * @return
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.wtf(TAG, "onStopJob id=" + params.getJobId());
        boolean shouldReschedule = updatePing.stopJob(params);
        return shouldReschedule;
    }

    /**
     * Internal private asynchronous task handling incoming JobService Tasks.
     */
    private class UpdatePingAsyncTask extends AsyncTask<JobParameters, Void, JobParameters[]>{

        WeakReference<Activity> mWeakActivity;

        public UpdatePingAsyncTask(Activity activity){
            mWeakActivity = new WeakReference<Activity>(activity);
        }

        public UpdatePingAsyncTask() {

        }

        /**
         * Invoked in background thread, the parameters of the asynchronous task are passed to this
         * step, computed/recomputed and then passed onto the onPostExecute.
         * @param params
         * @return
         */
        @Override
        protected JobParameters[] doInBackground(JobParameters... params) {
            JobParameters par = params[0];
            Log.wtf(TAG, "doInBackground proccessing...");
            Log.wtf(TAG, "Executing job=" + par.getJobId() + "in background");

            Runtime runtime = Runtime.getRuntime();
            Process process = null;
            try {
                process = runtime.exec("ping -c 1" + host);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int result = process.exitValue();
            return params;
        }

        /**
         * Runs on the UI thread after doInBackground()
         * Updates the ping and finishes the JobService by requesting post-execution rescheduling,
         * back-off timer will be applied if supplied if specified at schedule time.
         * @param result
         */
        @Override
        protected void onPostExecute(JobParameters[] result){
            for(JobParameters params : result) {
                Log.wtf(TAG, "opPostExecute, fetching UI...");
                Activity activity = mWeakActivity.get();
                if (activity != null) {
                    Log.wtf(TAG, "Activity succesfully fetched, updating view with ping...");
                    TextView ping = (TextView) activity.findViewById(R.id.pingbox);
                    ping.setText(pingresult);
                }
                Log.wtf(TAG, "finishing job with id=" + params.getJobId());
                jobFinished(params, true);
            }
        }

        /**
         * Logic for stopping a job. Return true if job should be rescheduled.
         * In this chase this would mean specifying a timeOut as the Periodic  repeat interval,
         * either the job can be rescheduled or return a default value to the UI, for example
         * returning: "latency > 1000ms" if the timeOut is meet.
         * @param params
         * @return
         */
        public boolean stopJob(JobParameters params){
            Log.wtf(TAG, "stopJob id=" + params.getJobId());
            Log.wtf(TAG, "reSchedule job..");
            return true;
        }
    }
}
