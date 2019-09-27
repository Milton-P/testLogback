package com.mt.syslogsdk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.mt.log.SmartLog;

//logback 介绍 https://logback.qos.ch/manual/introduction.html
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        SmartLog.getInstance().setLogFilePath("/data/data/com.mt.syslogsdk/files/testapp.log");
        SmartLog.getInstance().setServerHost("0.10.50.83");
        SmartLog.getInstance().setServerPort(10544);
        SmartLog.getInstance().init();
        Button button = findViewById(R.id.test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(()-> {
                    test1();
                }).start();
            }
        });
    }

    private void test1() {
        SmartLog.infoToAll("infoToAll no tag1");
        SmartLog.infoToFile("infoToFile no tag1");
        SmartLog.infoToLogcat("infoToLogcat no tag1");
        SmartLog.infoToServer("infoToServer no tag1");

        SmartLog.infoToAll("tag_infoToAll","infoToAll with tag1");
        SmartLog.infoToFile("tag_infoToFile","infoToFile with tag1");
        SmartLog.infoToLogcat("tag_infoToLogcat","infoToLogcat with tag1");
        SmartLog.infoToServer("tag_infoToServer","infoToServer with tag1");
    }
}
