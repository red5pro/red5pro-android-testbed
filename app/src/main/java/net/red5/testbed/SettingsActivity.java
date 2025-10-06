package net.red5.testbed;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "Red5ProSettings";
    private static final String KEY_STREAM_MANAGER_HOST = "stream_manager_host";
    private static final String KEY_STANDALONE_SERVER_IP = "standalone_server_ip";
    private static final String KEY_APP_NAME = "app_name";
    private static final String KEY_NODE_GROUP = "node_group";
    private static final String KEY_STREAM_NAME = "stream_name";
    private static final String KEY_USER_NAME = "username";
    private static final String KEY_PASSWORD = "password";

    private EditText etStreamManagerHost;
    private EditText etStandaloneServerIp;
    private EditText etAppName;
    private EditText etNodeGroup;
    private EditText etStreamName;
    private EditText etUserName;
    private EditText etPassword;


    private Button btnSave;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();

        loadSettings();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void initViews() {
        etStreamManagerHost = findViewById(R.id.et_stream_manager_host);
        etStandaloneServerIp = findViewById(R.id.et_standalone_server_ip);
        etAppName = findViewById(R.id.et_app_name);
        etNodeGroup = findViewById(R.id.et_node_group);
        etStreamName = findViewById(R.id.et_stream_name);
        etUserName = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save);
    }

    private void loadSettings() {
        String streamManagerHost = sharedPreferences.getString(KEY_STREAM_MANAGER_HOST, "");
        String standaloneServerIp = sharedPreferences.getString(KEY_STANDALONE_SERVER_IP, "");
        String appName = sharedPreferences.getString(KEY_APP_NAME, "live");
        String nodeGroup = sharedPreferences.getString(KEY_NODE_GROUP, "default");
        String streamName = sharedPreferences.getString(KEY_STREAM_NAME, "myStream");
        String username = sharedPreferences.getString(KEY_USER_NAME, "");
        String password = sharedPreferences.getString(KEY_PASSWORD, "");

        etStreamManagerHost.setText(streamManagerHost);
        etStandaloneServerIp.setText(standaloneServerIp);
        etAppName.setText(appName);
        etNodeGroup.setText(nodeGroup);
        etStreamName.setText(streamName);
        etUserName.setText(username);
        etPassword.setText(password);
    }

    private void saveSettings() {
        String streamManagerHost = etStreamManagerHost.getText().toString().trim();
        String standaloneServerIp = etStandaloneServerIp.getText().toString().trim();
        String appName = etAppName.getText().toString().trim();
        String nodeGroup = etNodeGroup.getText().toString().trim();
        String streamName = etStreamName.getText().toString().trim();
        String userName = etUserName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (appName.isEmpty()) appName = "live";
        if (nodeGroup.isEmpty()) nodeGroup = "default";
        if (streamName.isEmpty()) streamName = "myStream";

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_STREAM_MANAGER_HOST, streamManagerHost);
        editor.putString(KEY_STANDALONE_SERVER_IP, standaloneServerIp);
        editor.putString(KEY_APP_NAME, appName);
        editor.putString(KEY_NODE_GROUP, nodeGroup);
        editor.putString(KEY_STREAM_NAME, streamName);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_PASSWORD, password);


        editor.apply();

        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveSettings();
    }

    public static String getStreamManagerHost(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_STREAM_MANAGER_HOST, "");
    }

    public static String getStandaloneServerIp(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_STANDALONE_SERVER_IP, "");
    }

    public static String getAppName(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_APP_NAME, "live");
    }

    public static String getNodeGroup(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_NODE_GROUP, "default");
    }

    public static String getStreamName(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_STREAM_NAME, "myStream");
    }

    public static String getUserName(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }

    public static String getPassword(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_PASSWORD, "myStream");
    }



}
