package ua.ccbackup.serhiyshkurin;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Activity elements
        Button backupButton = (Button) findViewById(R.id.backupButton);
        Button restoreButton = (Button) findViewById(R.id.restoreButton);


        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BackupAccountsChooseActivity.class);
                startActivity(intent);
            }
        });
//TODO: Uncomment restoreButton listener when restoreAccountsChooseActivity will be ready.
        /*restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, restoreAccountsChooseActivity.class);
                startActivity(intent);
            }
        });*/
    }


}
