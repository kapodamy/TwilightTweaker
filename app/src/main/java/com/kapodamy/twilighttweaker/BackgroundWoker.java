package com.kapodamy.twilighttweaker;
import android.os.AsyncTask;

/**
 * Created by kapodamy on 15/03/2018.
 */
public class BackgroundWoker<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private final TaskListener<Params, Result> listener;

    public BackgroundWoker(TaskListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        listener.onTaskStarted();
    }

    @Override
    protected Result doInBackground(Params... params) {
        return listener.doTask(params);
    }

    @Override
    protected void onPostExecute(Result result) {
        listener.onTaskFinished(result);
    }
}
