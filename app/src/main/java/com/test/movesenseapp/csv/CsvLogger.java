package com.test.movesenseapp.csv;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.test.movesenseapp.R;
import com.test.movesenseapp.SampleApp;
import com.test.movesenseapp.data_manager.StorageUploader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

public class CsvLogger {

    private final String TAG = com.test.movesenseapp.csv.CsvLogger.class.getSimpleName();

    private final StringBuilder mStringBuilder;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 99;

    private boolean isHeaderExists = false;

    public CsvLogger() {
        mStringBuilder = new StringBuilder();
    }

    public void appendHeader(String header) {
        if (!isHeaderExists) {
            mStringBuilder.append(header);
            mStringBuilder.append("\n");
        }

        isHeaderExists = true;
    }

    public void appendLine(String line) {
        mStringBuilder.append(line);
        mStringBuilder.append("\n");
    }

    public void finishSavingLogs(Context context, String sensorName) {
        try {
            //create file
            File file = createLogFile(sensorName);

            //add contents to file
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(mStringBuilder.toString());
            fileWriter.close();

            //upload file
            StorageUploader uploader = new StorageUploader();
            uploader.uploadFileFirebase(file);

           // MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File createLogFile(String sensorName) {
        if (isExternalStorageWritable()) {
            File externalDirectory = Environment.getExternalStorageDirectory();
            File appDirectory = new File(externalDirectory, "SMRL");
            File logFile = new File(appDirectory, createFileNameIso8601(sensorName) + ".csv");

            // create app folder
            if (!appDirectory.exists()) {
                boolean status = appDirectory.mkdirs();
                Log.e(TAG, "appDirectory created: " + status);
            }

            // create log file
            if (!logFile.exists()) {
                boolean status = false;
                try {
                    status = logFile.createNewFile();
                    return logFile;
                } catch (IOException e) {
                    Log.e(TAG, "logFile.createNewFile(): ", e);
                    e.printStackTrace();
                }
                Log.e(TAG, "logFile.createNewFile() created: " + status);
            } else {
                return logFile;
            }
        } else {
            Log.e(TAG, "createFile isExternalStorageWritable Error");
        }
        return null;
    }

    private String createFileNameIso8601(String tag) {
        // timestamp (ISO 8601) + device serial + data type,
        StringBuilder sb = new StringBuilder();

        // Get Current Timestamp ISO 8601
        String currentISO8601Timestamp = String.format("%tFT%<tTZ.%<tL",
                Calendar.getInstance(TimeZone.getTimeZone("Z")));

        // Get connected device serial
        String deviceName = "Unknown";
        if (MovesenseConnectedDevices.getConnectedDevices().size() > 0) {
            deviceName = MovesenseConnectedDevices.getConnectedDevice(0).getSerial();
        }

        sb.append(currentISO8601Timestamp).append("_").
                append(deviceName).append("_")
                .append(tag).append("_").append(SampleApp.getUser().getId());

        return sb.toString();
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean checkRuntimeWriteExternalStoragePermission(Context context, final Activity activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.write_external_storage_permission_title)
                        .setMessage(R.string.write_external_storage_permission_text)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                requestForWriteExternalStoragePermission(activity);
                            }
                        })
                        .create()
                        .show();

            } else {
                requestForWriteExternalStoragePermission(activity);
            }
            Log.e(TAG, "checkRuntimeWriteExternalStoragePermission() FALSE");
            return false;
        } else {
            Log.e(TAG, "checkRuntimeWriteExternalStoragePermission() TRUE");
            return true;
        }
    }

    private void requestForWriteExternalStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }
}
