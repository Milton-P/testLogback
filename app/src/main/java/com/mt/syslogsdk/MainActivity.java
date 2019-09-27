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
                test1();
            }
        });
    }

    private void test1() {
        android.util.Log.d("milton", "data test test1");
        SmartLog.infoToAll("data test infoToAll no tag1");
        SmartLog.infoToFile("data test infoToFile no tag1");
        SmartLog.infoToLogcat("data test infoToLogcat no tag1");
        SmartLog.infoToServer("data test infoToServer no tag1");

        SmartLog.debugToAll("data test  debugToAll no tag1");
        SmartLog.debugToFile("data test idebugToFile no tag1");
        SmartLog.debugToLogcat("data test debugToLogcat no tag1");
        SmartLog.debugToServer("data test debugToServer no tag1");

        SmartLog.errorToAll("data test  errorToAll no tag1");
        SmartLog.errorToFile("data test errorToFile no tag1");
        SmartLog.errorToLogcat("data test errorToLogcat no tag1");
        SmartLog.errorToServer("data test errorToServer no tag1");

        SmartLog.infoToAll("tag_infoToAll","data test infoToAll with tag1");
        SmartLog.infoToFile("tag_infoToFile","data test  infoToFile with tag1");
        SmartLog.infoToLogcat("tag_infoToLogcat","data test  infoToLogcat with tag1");
        SmartLog.infoToServer("tag_infoToServer","data test infoToServer with tag1");

        SmartLog.debugToAll("tag_debugToAll", "data test  debugToAll with tag1");
        SmartLog.debugToFile("tag_debugToFile", "data test idebugToFile with tag1");
        SmartLog.debugToLogcat("tag_debugToLogcat", "data test debugToLogcat with tag1");
        SmartLog.debugToServer("tag_debugToServer" ,"data test debugToServer with tag1");

        SmartLog.errorToAll("tag_errorToAll", "data test  errorToAll with tag1");
        SmartLog.errorToFile("tag_errorToFile", "data test errorToFile with tag1");
        SmartLog.errorToLogcat("tag_errorToLogcat",  "data test errorToLogcat with tag1");
        SmartLog.errorToServer("tag_errorToServer", "data test errorToServer with tag1");
    }
}
