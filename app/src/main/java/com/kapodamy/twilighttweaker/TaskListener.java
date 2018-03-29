package com.kapodamy.twilighttweaker;

/**
 * Created by kapodamy on 15/03/2018.
 */

interface TaskListener<Params, Result> {
    void onTaskStarted();
    Result doTask(Params[] params);
    void onTaskFinished(Result result);
}
