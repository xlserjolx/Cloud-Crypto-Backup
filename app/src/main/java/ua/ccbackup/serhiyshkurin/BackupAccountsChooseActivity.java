package ua.ccbackup.serhiyshkurin;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.drive.Drive;


import java.io.File;
import java.util.ArrayList;

import paul.arian.fileselector.FileSelectionActivity;

public class BackupAccountsChooseActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String FILES_TO_UPLOAD = "upload";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private static final int REQUEST_CODE_FILE_CHOOSER = 12798;

    private static final String TAG = "myTag";
    protected static GoogleApiClient googleApiClient = null;

    private ToggleButton gdButton;
    private TextView filesTextList;
    private TextView magicTextView;

    private String[] fileNamesArray;
    private String[] filesPathArray;
    private ArrayList<Object> connectedAccountsList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_backup_accounts_choose);

            filesTextList = (TextView) findViewById(R.id.selectedFilesList);
            magicTextView = (TextView) findViewById(R.id.backupMagicField);
            gdButton = (ToggleButton) findViewById(R.id.GD_button);
            //ydButton = (ToggleButton) findViewById(R.id.YD_button);
            //dbButton = (ToggleButton) findViewById(R.id.DB_button);

        Button continueButton = (Button) findViewById(R.id.backupContinueButton);
        Button chooseFilesButton = (Button) findViewById(R.id.backupFilesChooseButton);

            chooseFilesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    runChooseFileDialog();
                }
            });

            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startBackup();
                }
            });

            gdButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.i(TAG, "onClickGDButton is called");

                    if (buttonView.isChecked()) {
                        Log.i(TAG, "Google Drive button is activated");

                        buildGoogleApiClient();
                        googleApiClient.connect();

                        //TODO: 3) Add ToggleButton background image
                    } else {
                        Log.i(TAG, "Google Drive button is deactivated");
                        googleApiClient.disconnect();

                        //TODO: 3) Remove ToggleButton background image
                    }
                }
            });
                //
                //
            mResolvingError = savedInstanceState != null
                    && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
    }

    private void startBackup(){
        if(connectedAccountsCheck()) {
            Intent intent = new Intent(this, FileRouletteActivity.class);
            Log.i(TAG, "Sending from backupAccountsChooseAtivity with package name: " + getPackageName());
            intent.putExtra(getPackageName() + "FILES_LIST", filesPathArray);
            Log.i(TAG, "START_BACKUP: filesPathArray size: " + filesPathArray.length);
            intent.putExtra(getPackageName() + "FILENAME_LIST", fileNamesArray);
            Log.i(TAG, "START_BACKUP: fileNamesArray size: " + fileNamesArray.length);
            intent.putExtra(getPackageName() + "MAGIC_SIGNS", magicTextView.getText().toString());
            //Maybe something else
            startActivity(intent);

        }else{
            //TODO: Show message "No accounts connected" dialog
        }
    }

    private boolean connectedAccountsCheck() {
        connectedAccountsList.clear();
        Boolean res = false;
        if(!(googleApiClient == null)){
            connectedAccountsList.add(0, googleApiClient);
            res=true;
        }
        else connectedAccountsList.add(0, null);
        //if(!(someYandexApiClient == null))connectedAccountsList.add(0, someYandexApiClient);
        //else connectedAccountsList.add(0, null);
        //if(!(dropboxApiClient == null))connectedAccountsList.add(0, dropboxApiClient);
        //else connectedAccountsList.add(0, null);
        //TODO: 1) Check for authorized accounts
        //TODO: 2) Build connectedAccountsList
        return res;
    }

    private void runChooseFileDialog() {
            try {
                Intent intent = new Intent(this, FileSelectionActivity.class);
                startActivityForResult(intent, REQUEST_CODE_FILE_CHOOSER);
                Log.i(TAG, "Expected intent STARTED");
            } catch (Exception e) {
                Intent photoPickerIntent = new Intent(this, this.getClass());
                startActivityForResult(photoPickerIntent, REQUEST_CODE_FILE_CHOOSER);
                Log.i(TAG, "photoPickerIntent STARTED");
            }
        }
    protected int googlePlayServicesAvailabilityCheckCode(){
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
            switch (googlePlayServicesAvailabilityCheckCode()) {
                case ConnectionResult.SUCCESS: {
                    Log.i(TAG, "Google API connection SUCCESS");
                    gdButton.setChecked(true);
                    break;
                }
                case ConnectionResult.SERVICE_MISSING:
                    Log.i(TAG, "Google API connection SERVICE_MISSING");
                case ConnectionResult.SERVICE_UPDATING:
                    Log.i(TAG, "Google API connection SERVICE_UPDATING");
                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                    Log.i(TAG, "Google API connection SERVICE_VERSION_UPDATE_REQUIRED");
                case ConnectionResult.SERVICE_DISABLED:
                    Log.i(TAG, "Google API connection SERVICE_DISABLED");
                case ConnectionResult.SERVICE_INVALID:
                    Log.i(TAG, "Google API connection SERVICE_INVALID");
                default:
                    Log.i(TAG, "Google API connection UNEXPECTED ERROR");
            }
    }

    /*build the google api client*/
    private void buildGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        // Connected to Google Play services!
        // The good stuff goes here.
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        switch(result.getErrorCode()){
            case ConnectionResult.SERVICE_MISSING: Log.i(TAG, "Google API Connection Failed: SERVICE_MISSING");
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED: Log.i(TAG, "Google API Connection Failed: SERVICE_VERSION_UPDATE_REQUIRED");
            case ConnectionResult.SERVICE_DISABLED: Log.i(TAG, "Google API Connection Failed: SERVICE_DISABLED");
            case ConnectionResult.SIGN_IN_REQUIRED: Log.i(TAG, "Google API Connection Failed: SIGN_IN_REQUIRED(user account has not been specified)");
            default: Log.i(TAG, "UNEXPECTED ERROR: " + result.getErrorMessage());
        }

        if (mResolvingError) {
            // Already attempting to resolve an error.
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
        public ErrorDialogFragment() { }

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

        if ((requestCode == REQUEST_CODE_FILE_CHOOSER) && resultCode == RESULT_OK) {
            Log.i(TAG, "resultCode=" + resultCode);
            Log.i(TAG, "data=" + String.valueOf(data));
            ArrayList<File> files = (ArrayList<File>) data.getSerializableExtra(FILES_TO_UPLOAD); //file array list
            Log.i(TAG, "firstArrayElement " + files.get(0).toString());
            //Log.i(TAG, "secondArrayElement " + files.get(1).toString());
            filesTextList.clearComposingText();
            fileNamesArray = new String[files.size()];
            filesPathArray = new String[files.size()];
            for(int j = 0;j<files.size(); j++){
                Log.i(TAG, "Adding path: "+files.get(j).toString());
                fileNamesArray[j]=files.get(j).getName();
                filesPathArray[j]=files.get(j).toString(); //storing the selected file's paths to string array filesPathArray
                Log.i(TAG, "Added path: " + filesPathArray[j]);
                filesTextList.append(files.get(j).getName()+"; ");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }
}
