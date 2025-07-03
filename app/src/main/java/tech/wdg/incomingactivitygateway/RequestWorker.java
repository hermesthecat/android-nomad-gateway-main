package tech.wdg.incomingactivitygateway;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RequestWorker extends Worker {

    public final static String DATA_URL = "URL";
    public final static String DATA_TEXT = "TEXT";
    public final static String DATA_HEADERS = "HEADERS";
    public final static String DATA_IGNORE_SSL = "IGNORE_SSL";
    public final static String DATA_MAX_RETRIES = "MAX_RETRIES";
    public final static String DATA_CHUNKED_MODE = "CHUNKED_MODE";
    public final static String DATA_FORWARDING_TYPE = "FORWARDING_TYPE";
    public final static String DATA_FORWARDING_NUMBER = "FORWARDING_NUMBER";

    public RequestWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int maxRetries = getInputData().getInt(DATA_MAX_RETRIES, 10);

        if (getRunAttemptCount() > maxRetries) {
            return Result.failure();
        }

        String forwardingType = getInputData().getString(DATA_FORWARDING_TYPE);
        if (forwardingType != null && forwardingType.equals(ForwardingConfig.ForwardingType.SMS.getValue())) {
            // Handle SMS Forwarding
            String phoneNumber = getInputData().getString(DATA_FORWARDING_NUMBER);
            String message = getInputData().getString(DATA_TEXT);

            if (phoneNumber == null || message == null) {
                return Result.failure();
            }

            try {
                SmsSender.send(getApplicationContext(), phoneNumber, message);
                return Result.success();
            } catch (Exception e) {
                // Log the exception, maybe retry? For now, we fail.
                return Result.failure();
            }

        } else {
            // Handle Webhook Forwarding (existing logic)
            String url = getInputData().getString(DATA_URL);
            String text = getInputData().getString(DATA_TEXT);
            String headers = getInputData().getString(DATA_HEADERS);
            boolean ignoreSsl = getInputData().getBoolean(DATA_IGNORE_SSL, false);
            boolean useChunkedMode = getInputData().getBoolean(DATA_CHUNKED_MODE, true);

            Request request = new Request(url, text);
            request.setJsonHeaders(headers);
            request.setIgnoreSsl(ignoreSsl);
            request.setUseChunkedMode(useChunkedMode);

            String result = request.execute();

            if (result.equals(Request.RESULT_RETRY)) {
                return Result.retry();
            }

            if (result.equals(Request.RESULT_ERROR)) {
                return Result.failure();
            }

            return Result.success();
        }
    }
}
