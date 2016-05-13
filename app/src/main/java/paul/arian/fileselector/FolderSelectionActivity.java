package paul.arian.fileselector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import ua.ccbackup.serhiyshkurin.R;

public class FolderSelectionActivity extends Activity {

    private static final String FILES_TO_UPLOAD = "upload";
    private File mainPath = new File(Environment.getExternalStorageDirectory()+"");

    private ListView directoryView;
    private ArrayList<File> directoryList = new ArrayList<>();
    private ArrayList<String> directoryNames = new ArrayList<>();
    private ArrayList<String> fileNames = new ArrayList<>();
    private Button ok;
    private Button all;
    private Button cancel;
    private Button storage;
    private Button New;
    private TextView path;




    private Boolean switcher = false;
    private String primary_sd;
    private String secondary_sd;

    private int index = 0;
    private int top = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_selection);
        //getActionBar().setDisplayHomeAsUpEnabled(true);

        directoryView = (ListView)findViewById(R.id.directorySelectionList);
        ok = (Button)findViewById(R.id.ok);
        all = (Button)findViewById(R.id.all);
        cancel = (Button)findViewById(R.id.cancel);
        storage = (Button)findViewById(R.id.storage);
        New = (Button)findViewById(R.id.New);
        path = (TextView)findViewById(R.id.folderpath);

        all.setEnabled(false);


        loadLists();

        ExtStorageSearch();
        if(secondary_sd==null){
            storage.setEnabled(false);
        }

        directoryView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                index = directoryView.getFirstVisiblePosition();
                View v = directoryView.getChildAt(0);
                top = (v == null) ? 0 : v.getTop();

                File lastPath = mainPath;
                try {
                    if (position < directoryList.size()) {
                        mainPath = directoryList.get(position);
                        loadLists();
                    }
                }catch(Throwable e){
                    mainPath = lastPath;
                    loadLists();
                }
            }
        });

        ok.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                ok();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                finish();
            }
        });

        storage.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                try {
                    if (!switcher) {
                        mainPath = new File(secondary_sd);
                        loadLists();
                        switcher = true;
                        storage.setText(getString(R.string.Int));
                    } else {
                        mainPath = new File(primary_sd);
                        loadLists();
                        switcher = false;
                        storage.setText(getString(R.string.ext));
                    }
                }catch (Throwable e){

                }
            }
        });

        New.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                alert.setTitle( getString(R.string.New) );
                alert.setMessage( getString(R.string.CNew) );

                final EditText input = new EditText(v.getContext());
                alert.setView(input);

                alert.setPositiveButton(getString(R.string.create), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String fileName = input.getText().toString();
                        // Verify if a value has been entered.
                        if(fileName != null && fileName.length() > 0) {
                            // Notify the listeners.
                            File newFolder = new File(mainPath.getPath()+"/"+fileName+"/");
                            newFolder.mkdirs();
                            loadLists();
                        }
                    }
                });
                alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing, automatically the dialog is going to be closed.
                    }
                });

                // Show the dialog.
                alert.show();

            }
        });
    }

    @Override
    public void onBackPressed() {
        try {
            if(mainPath.equals(Environment.getExternalStorageDirectory().getParentFile().getParentFile())){
                finish();
            }else{
                File parent = mainPath.getParentFile();
                mainPath = parent;
                loadLists();
                directoryView.setSelectionFromTop(index, top);
            }

        }catch (Throwable e){

        }
    }

    private void ok(){
        Intent result = this.getIntent();
        result.putExtra(FILES_TO_UPLOAD, mainPath);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private void loadLists(){
        FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        };
        FileFilter directoryFilter = new FileFilter(){
            public boolean accept(File file){
                return file.isDirectory();
            }
        };

        //if(mainPath.exists() && mainPath.length()>0){
        //Lista de directorios
        File[] tempDirectoryList = mainPath.listFiles(directoryFilter);

        if (tempDirectoryList != null && tempDirectoryList.length > 1) {
            Arrays.sort(tempDirectoryList, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return object1.getName().compareTo(object2.getName());
                }
            });
        }


        directoryList = new ArrayList<>();
        directoryNames = new ArrayList<>();
        for(File file: tempDirectoryList){
            directoryList.add(file);
            directoryNames.add(file.getName());
        }
        ArrayAdapter<String> directoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, directoryNames);
        directoryView.setAdapter(directoryAdapter);

        //Lista de ficheros
        File[] tempFileList = mainPath.listFiles(fileFilter);

        if (tempFileList != null && tempFileList.length > 1) {
            Arrays.sort(tempFileList, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return object1.getName().compareTo(object2.getName());
                }
            });
        }

        ArrayList<File> fileList = new ArrayList<>();
        fileNames = new ArrayList<>();
        for(File file : tempFileList){
            fileList.add(file);
            fileNames.add(file.getName());
        }



        path.setText(mainPath.toString());
        iconload();
        // }
    }

    private void iconload(){
        String[] foldernames = new String[directoryNames.size()];
        foldernames = directoryNames.toArray(foldernames);

        String[] filenames = new String[fileNames.size()];
        filenames = fileNames.toArray(filenames);

        CustomListSingleOnly adapter1 = new CustomListSingleOnly(FolderSelectionActivity.this, directoryNames.toArray(foldernames), mainPath.getPath());
        CustomListSingleOnly adapter2 = new CustomListSingleOnly(FolderSelectionActivity.this, fileNames.toArray(filenames), mainPath.getPath());


        MergeAdapter adap = new MergeAdapter();

        adap.addAdapter(adapter1);
        adap.addAdapter(adapter2);


        directoryView.setAdapter(adap);
    }

    private void ExtStorageSearch(){
        String[] extStorlocs = {"/storage/sdcard1","/storage/extsdcard","/storage/sdcard0/external_sdcard","/mnt/extsdcard",
                "/mnt/sdcard/external_sd","/mnt/external_sd","/mnt/media_rw/sdcard1","/removable/microsd","/mnt/emmc",
                "/storage/external_SD","/storage/ext_sd","/storage/removable/sdcard1","/data/sdext","/data/sdext2",
                "/data/sdext3","/data/sdext4","/storage/sdcard0"};

        //First Attempt
        primary_sd = System.getenv("EXTERNAL_STORAGE");
        secondary_sd = System.getenv("SECONDARY_STORAGE");


        if(primary_sd == null) {
            primary_sd = Environment.getExternalStorageDirectory()+"";
        }
        if(secondary_sd == null) {//if fail, search among known list of extStorage Locations
            for(String string: extStorlocs){
                if((new File(string)).exists() && (new File(string)).isDirectory() ){
                    secondary_sd = string;
                    break;
                }
            }
        }

    }



}