package ua.ccbackup.serhiyshkurin;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class FileRouletteActivity extends BackupAccountsChooseActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static String[] filesPathArray;
    private String[] fileNamesList;
    private String magicSpell;
    private ArrayList<Object> connectedAccountsList = new ArrayList<>();
    private File tempFile;
    private int rouletteCounter = 0;
    private FileInputStream fileInputStream;
    private ArrayList<byte[]> fileByteArraysList = new ArrayList<>();
    private static final String TAG = "myTag";
    public static String appName;
    private Button startButton;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private static DriveFolder driveFolder;

    private String cloudFolderName;
    private static final int REQUEST_CODE_CREATOR = 2;

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_roulette);

        appName = getString(R.string.app_name);
        cloudFolderName = null;

        if (googleApiClient != null)
            Log.i(TAG, "mGoogleApiClient kept");
        else
            Log.e(TAG, "mGoogleApiClient DON'T kept");
        //mYandexApiClient=BackupAccountsChooseActivity.yandexApiClient;
        //mDropboxApiClient=BackupAccountsChooseActivity.dropboxApiClient;
        Log.i(TAG, "Getting from backupAccountsChooseAtivity to package name:" + getPackageName());
        filesPathArray = getIntent().getStringArrayExtra(getPackageName() + "FILES_LIST");
        Log.i(TAG, "FILES_LIST size: " + filesPathArray.length);
        fileNamesList = getIntent().getStringArrayExtra(getPackageName() + "FILENAME_LIST");
        Log.i(TAG, "FILENAME_LIST size: " + fileNamesList.length);
        magicSpell = getIntent().getStringExtra(getPackageName() + "MAGIC_SIGNS");
        //TODO: Delete next log message code
        Log.i(TAG, "MAGIC_SIGNS: " + magicSpell);

        //Checking list elements
        if (!(googleApiClient == null)) connectedAccountsList.add(googleApiClient);
        //if(!(mYandexApiClient==null))connectedAccountsList.add(mYandexApiClient);
        //if(!(mDropboxApiClient==null))connectedAccountsList.add(mDropboxApiClient);
        if (!(googleApiClient == null)/*||!(yandexApiClient==null)||!(dropboxApiClient==null))*/)
            startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runRoulette();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        switch (result.getErrorCode()) {
            case ConnectionResult.SERVICE_MISSING:
                Log.e(TAG, "Google API Connection Failed: SERVICE_MISSING");
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Log.e(TAG, "Google API Connection Failed: SERVICE_VERSION_UPDATE_REQUIRED");
            case ConnectionResult.SERVICE_DISABLED:
                Log.e(TAG, "Google API Connection Failed: SERVICE_DISABLED");
            case ConnectionResult.SIGN_IN_REQUIRED:
                Log.e(TAG, "Google API Connection Failed: SIGN_IN_REQUIRED(user account has not been specified)");
            default:
                Log.e(TAG, "UNEXPECTED ERROR: " + result.getErrorMessage());
        }

        if (mResolvingError) {
            // Already attempting to resolve an error.
            Log.e(TAG, "onConnectionFailed ERROR - resolved");
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                googleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    protected void onDialogDismissed() {
        mResolvingError = false;
    }


    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((BackupAccountsChooseActivity) getActivity()).onDialogDismissed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleApiClient.isConnecting() &&
                        !googleApiClient.isConnected()) {
                    googleApiClient.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    private void runRoulette() {
        Log.i(TAG, "runRoulette: rouletteCounter = " + rouletteCounter);
        Log.i(TAG, "runRoulette: filesPathArray.length = " + filesPathArray.length);
        if (rouletteCounter < filesPathArray.length) {
            tempFile = new File(filesPathArray[rouletteCounter]);
            encodingFile(tempFile);
        } else {
            Log.i(TAG, "Operations completed!!!!!!!");
            //TODO: Operations completed
        }
    }

    private void encodingFile(File file1) {
        Log.i(TAG, "RUN: Encoding file...");
        byte tempByte;
        int jump;
        int cursor = 0;
        try {
            fileInputStream = new FileInputStream(file1);
            int bytesQuantity = fileInputStream.available();
            byte[] fileByteArrayEncoding = new byte[bytesQuantity + 1];
            //fileInputStream.read(fileByteArrayEncoding);
            Log.i(TAG, "ENCODING_FILE: File added to array");
            for (int byteCursor = 0; byteCursor < bytesQuantity; byteCursor++) {
                if (cursor >= magicSpell.length()) cursor = 0;
                jump = magicSpell.charAt(cursor);
                //TODO: Delete next log message code
                Log.i(TAG, "ENCODING_FILE: Magic - " + jump);
                if ((byteCursor + jump) <= bytesQuantity) {
                    tempByte = fileByteArrayEncoding[byteCursor];
                    fileByteArrayEncoding[byteCursor] = fileByteArrayEncoding[byteCursor + jump];
                    fileByteArrayEncoding[byteCursor + jump] = tempByte;
                    //TODO: Delete next log message code
                    Log.i(TAG, "ENCODING_FILE: Exchanged " + byteCursor + " and " + (byteCursor + jump));
                } else {
                    fileByteArrayEncoding[bytesQuantity] = (byte) cursor;
                    Log.i(TAG, "ENCODING_FILE: Last cursor = " + cursor);
                    break;
                }
                cursor++;
            }

            Log.i(TAG, "ENCODING_FILE: DONE!!!");
            mergingFile(fileByteArrayEncoding);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "ENCODING_FILE: File not found");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "ENCODING_FILE: IOException " + e.toString());
        } finally {
            try {
                fileInputStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void mergingFile(byte[] fileByteArrayMerging) {
        Log.i(TAG, "RUN: Merging file...");
        int tail;
        int step;

        tail = fileByteArrayMerging.length % connectedAccountsList.size();
        step = ((fileByteArrayMerging.length - tail) / connectedAccountsList.size());

        byte[] tempFileByteArray;
        for (int i = 1; i <= connectedAccountsList.size(); i++) {
            if (i < connectedAccountsList.size()) {
                tempFileByteArray = new byte[step];
                if (i == 1)
                    System.arraycopy(fileByteArrayMerging, 0, tempFileByteArray, 0, step + 1);
                else
                    System.arraycopy(fileByteArrayMerging, step * (i - 1), tempFileByteArray, step * (i - 1), step * i + 1 - step * (i - 1));
            } else {
                tempFileByteArray = new byte[step + tail];
                System.arraycopy(fileByteArrayMerging, step * (i - 1), tempFileByteArray, step * (i - 1), fileByteArrayMerging.length - step * (i - 1));
            }
            fileByteArraysList.add(tempFileByteArray);
        }
        Log.i(TAG, "Merging file: DONE!!!");

        uploadCuts(fileByteArraysList);
    }


    public void uploadCuts(final ArrayList<byte[]> fileByteArraysList) {
        Log.i(TAG, "RUN: Uploading file...");
        if (cloudFolderName == null) {
            cloudFolderName = (appName + "_" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "." + Calendar.getInstance().get(Calendar.MONTH) + "." + Calendar.getInstance().get(Calendar.YEAR) + "_" + Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":" + Calendar.getInstance().get(Calendar.MINUTE));
            createFolders(cloudFolderName);
        }else {
            for (int i = 0; i < connectedAccountsList.size(); i++) {
                createFile(driveFolder, fileNamesList[i], appName, fileByteArraysList.get(i));
            }
            Log.i(TAG, "RUN: Uploading file DONE!!!");
            rouletteCounter++;
            runRoulette();
        }

    }


        /***********************************************************************
         * create file in GDrive
         * @param driveFolder1 parent's folder, (null for root)
         * @param titl  file name
         * @param mime  file mime type
         * @param fileByteArray2  ByteArraysList to create new file from it
         */

    void createFile(DriveFolder driveFolder1, final String titl, final String mime, final byte[] fileByteArray2) {
        Log.i(TAG, "createFile Parameters: "+driveFolder1+"/nTitle: "+titl+"/nMIME TYPE: "+mime);
        if (googleApiClient != null && googleApiClient.isConnected() && titl != null && mime != null && fileByteArray2!= null)
            try {
                final DriveFolder parent = driveFolder1 != null ? driveFolder1 : Drive.DriveApi.getRootFolder(googleApiClient);
                if (parent == null) return; //----------------->>>

                // Start by creating a new contents, and setting a callback.
                Log.i(TAG, "Creating new contents.");
                Drive.DriveApi.newDriveContents(googleApiClient)
                        .setResultCallback(new ResultCallback<DriveContentsResult>() {

                            @Override
                            public void onResult(DriveContentsResult result) {
                                // If the operation was not successful, we cannot do anything
                                // and must
                                // fail.
                                if (!result.getStatus().isSuccess()) {
                                    Log.i(TAG, "Failed to create new contents.");
                                    return;
                                }
                                // Otherwise, we can write our data to the new contents.
                                Log.i(TAG, "New contents created.");
                                // Get an output stream for the contents.
                                OutputStream outputStream = result.getDriveContents().getOutputStream();
                                // Write the bitmap data from it.
                                //ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                try {
                                    outputStream.write(fileByteArray2);
                                    outputStream.flush();
                                    outputStream.close();
                                } catch (IOException e1) {
                                    Log.i(TAG, "Unable to write file contents.");
                                }
                                // Create the initial metadata - MIME type and title.
                                // Note that the user will be able to change the title later.
                                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                        .setMimeType(mime)
                                        .setTitle(titl).build();
                                // Create an intent for the file chooser, and start it.
                                IntentSender intentSender = Drive.DriveApi
                                        .newCreateFileActivityBuilder()
                                        .setInitialMetadata(metadataChangeSet)
                                        .setInitialDriveContents(result.getDriveContents())
                                        .build(googleApiClient);
                                try {
                                    startIntentSenderForResult(
                                            intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.i(TAG, "Failed to launch file chooser.");
                                }
                            }
                        });
            }catch (Exception e){

            }
    }

    private void createFolders(final String foldersName) {
        //Creating Google Drive folder
                Log.i(TAG, "createFolders: Creating Google Drive folder..." + foldersName);
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(foldersName).build();
                Drive.DriveApi.getRootFolder(getGoogleApiClient()).createFolder(
                        getGoogleApiClient(), changeSet).setResultCallback(folderCreatedCallback);

    }

        ResultCallback<DriveFolder.DriveFolderResult> folderCreatedCallback = new
                ResultCallback<DriveFolder.DriveFolderResult>() {
                    @Override
                    public void onResult(DriveFolder.DriveFolderResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "createFolders: Error while trying to create Google Drive folder");
                            return;
                        }else{
                            driveFolder = Drive.DriveApi.getFolder(googleApiClient, result.getDriveFolder().getDriveId());
                            Log.i(TAG, "!!!!!!createFolders: Created Google Drive folder: " + result.getDriveFolder().getDriveId()+"\n\t"+cloudFolderName);
                            uploadCuts(fileByteArraysList);
                        }
                    }
                };



}
