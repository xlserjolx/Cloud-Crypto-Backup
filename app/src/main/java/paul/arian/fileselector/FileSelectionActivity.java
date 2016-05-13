package paul.arian.fileselector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ua.ccbackup.serhiyshkurin.R;

public class FileSelectionActivity extends Activity {

    private static final String TAG = "FileSelection";
    private static final String FILES_TO_UPLOAD = "upload";
    private File mainPath = new File(Environment.getExternalStorageDirectory()+"");

    private ListView directoryView;
    private ArrayList<File> directoryList = new ArrayList<>();
    private ArrayList<String> directoryNames = new ArrayList<>();
    //private ListView fileView;
    private ArrayList<File> fileList = new ArrayList<>();
    private ArrayList<String> fileNames = new ArrayList<>();
    private Button ok;
    private Button all;
    private Button cancel;
    private Button storage;
    private Button New;
    private TextView path;
    private Boolean Switch = false;

    private List<File> extStoragesList;

    private String primary_sd;
    private String secondary_sd;

    private int index = 0;
    private int top = 0;
    private int currentPathIndex = 0;

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


        loadLists();
        New.setEnabled(false);


        ExtStorageSearch();
        //if(secondary_sd==null){
            storage.setEnabled(false);
            //storage.setVisibility(View.INVISIBLE);
            //storage.setClickable(false);
        //}


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
                }catch (Throwable e){
                    mainPath = lastPath;
                    loadLists();
                }

            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
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
                        mainPath = fileList.get(currentPathIndex);
                        loadLists();
                        storage.setText(mainPath.getName()+"");

                }catch (Throwable e){

                }
            }
        });

        all.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(!Switch){
                    for (int i = directoryList.size(); i < directoryView.getCount(); i++){
                        directoryView.setItemChecked(i, true);
                    }
                    all.setText(getString(R.string.none));
                    Switch = true;
                }else {
                    for (int i = directoryList.size(); i < directoryView.getCount(); i++) {
                        directoryView.setItemChecked(i, false);
                    }
                    all.setText(getString(R.string.all));
                    Switch = false;
                }
            }

        });
    }

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
        Log.d(TAG, "Upload clicked, finishing activity");


        ArrayList<File> resultFileList = new ArrayList<>();

        for(int i = 0 ; i < directoryView.getCount(); i++){
            if(directoryView.isItemChecked(i)){
                resultFileList.add(fileList.get(i - directoryList.size()));
            }
        }
        if(resultFileList.isEmpty()){
            Log.i(TAG, "Empty selection");
            finish();
        }
        Log.i(TAG, "Files: "+ resultFileList.toString());
        Intent result = this.getIntent();
        result.putExtra(FILES_TO_UPLOAD, resultFileList);
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

        fileList = new ArrayList<>();
        fileNames = new ArrayList<>();
        for(File file : tempFileList){
            fileList.add(file);
            fileNames.add(file.getName());
        }



        path.setText(mainPath.toString());
        iconload();
        setTitle(mainPath.getName());
        //}
    }

    /**@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_file_selection, menu);
    return true;
    }
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
     switch (item.getItemId()) {
     case android.R.id.home:
     NavUtils.navigateUpFromSameTask(this);
     return true;
     }
     return super.onOptionsItemSelected(item);
     }**/

    private void iconload(){
        String[] foldernames = new String[directoryNames.size()];
        foldernames = directoryNames.toArray(foldernames);

        String[] filenames = new String[fileNames.size()];
        filenames = fileNames.toArray(filenames);

        CustomListSingleOnly adapter1 = new CustomListSingleOnly(FileSelectionActivity.this, directoryNames.toArray(foldernames), mainPath.getPath());
        CustomList adapter2 = new CustomList(FileSelectionActivity.this, fileNames.toArray(filenames), mainPath.getPath());


        MergeAdapter adap = new MergeAdapter();

        adap.addAdapter(adapter1);
        adap.addAdapter(adapter2);


        directoryView.setAdapter(adap);
    }

    private void ExtStorageSearch(){
        File extStoragePath = new File(Environment.getExternalStorageDirectory()+"");
        extStoragesList = Arrays.asList((extStoragePath.getParentFile()).listFiles());
        /*String[] extStorlocs = {"/storage/sdcard1","/storage/extsdcard","/storage/sdcard0/external_sdcard","/mnt/extsdcard",
                "/mnt/sdcard/external_sd","/mnt/external_sd","/mnt/media_rw/sdcard1","/removable/microsd","/mnt/emmc",
                "/storage/external_SD","/storage/ext_sd","/storage/removable/sdcard1","/data/sdext","/data/sdext2",
                "/data/sdext3","/data/sdext4","/storage/sdcard0"};*/

        //First Attempt
        primary_sd = System.getenv("EXTERNAL_STORAGE");
        secondary_sd = System.getenv("SECONDARY_STORAGE");


        if(primary_sd == null) {
            primary_sd = Environment.getExternalStorageDirectory()+"";
        }
        currentPathIndex = extStoragesList.indexOf(primary_sd);
        if(secondary_sd == null) {//if fail, search among known list of extStorage Locations
            for(File string: extStoragesList){
                if((new File(String.valueOf(string))).exists() && !(new File(String.valueOf(string))).isDirectory() ){
                    extStoragesList.remove(string);
                    break;
                }
            }
        }

    }



}