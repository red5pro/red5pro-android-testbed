package net.red5.testbed

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private var etStreamManagerHost: EditText? = null
    private var etStandaloneServerIp: EditText? = null
    private var etAppName: EditText? = null
    private var etNodeGroup: EditText? = null
    private var etStreamName: EditText? = null
    private var etUserName: EditText? = null
    private var etPassword: EditText? = null


    private var btnSave: Button? = null

    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        initViews()

        loadSettings()

        btnSave!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                saveSettings()
            }
        })
    }

    private fun initViews() {
        etStreamManagerHost = findViewById<EditText>(R.id.et_stream_manager_host)
        etStandaloneServerIp = findViewById<EditText>(R.id.et_standalone_server_ip)
        etAppName = findViewById<EditText>(R.id.et_app_name)
        etNodeGroup = findViewById<EditText>(R.id.et_node_group)
        etStreamName = findViewById<EditText>(R.id.et_stream_name)
        etUserName = findViewById<EditText>(R.id.et_username)
        etPassword = findViewById<EditText>(R.id.et_password)
        btnSave = findViewById<Button>(R.id.btn_save)
    }

    private fun loadSettings() {
        val streamManagerHost: String = sharedPreferences!!.getString(KEY_STREAM_MANAGER_HOST, "")!!
        val standaloneServerIp: String =
            sharedPreferences!!.getString(KEY_STANDALONE_SERVER_IP, "")!!
        val appName: String = sharedPreferences!!.getString(KEY_APP_NAME, "live")!!
        val nodeGroup: String = sharedPreferences!!.getString(KEY_NODE_GROUP, "default")!!
        val streamName: String = sharedPreferences!!.getString(KEY_STREAM_NAME, "myStream")!!
        val username: String = sharedPreferences!!.getString(KEY_USER_NAME, "")!!
        val password: String = sharedPreferences!!.getString(KEY_PASSWORD, "")!!

        etStreamManagerHost!!.setText(streamManagerHost)
        etStandaloneServerIp!!.setText(standaloneServerIp)
        etAppName!!.setText(appName)
        etNodeGroup!!.setText(nodeGroup)
        etStreamName!!.setText(streamName)
        etUserName!!.setText(username)
        etPassword!!.setText(password)
    }

    private fun saveSettings() {
        val streamManagerHost = etStreamManagerHost!!.getText().toString().trim { it <= ' ' }
        val standaloneServerIp = etStandaloneServerIp!!.getText().toString().trim { it <= ' ' }
        var appName = etAppName!!.getText().toString().trim { it <= ' ' }
        var nodeGroup = etNodeGroup!!.getText().toString().trim { it <= ' ' }
        var streamName = etStreamName!!.getText().toString().trim { it <= ' ' }
        val userName = etUserName!!.getText().toString().trim { it <= ' ' }
        val password = etPassword!!.getText().toString().trim { it <= ' ' }

        if (appName.isEmpty()) appName = "live"
        if (nodeGroup.isEmpty()) nodeGroup = "default"
        if (streamName.isEmpty()) streamName = "myStream"

        val editor = sharedPreferences!!.edit()
        editor.putString(KEY_STREAM_MANAGER_HOST, streamManagerHost)
        editor.putString(KEY_STANDALONE_SERVER_IP, standaloneServerIp)
        editor.putString(KEY_APP_NAME, appName)
        editor.putString(KEY_NODE_GROUP, nodeGroup)
        editor.putString(KEY_STREAM_NAME, streamName)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_PASSWORD, password)


        editor.apply()

        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveSettings()
    }

    companion object {
        private const val PREFS_NAME = "Red5ProSettings"
        private const val KEY_STREAM_MANAGER_HOST = "stream_manager_host"
        private const val KEY_STANDALONE_SERVER_IP = "standalone_server_ip"
        private const val KEY_APP_NAME = "app_name"
        private const val KEY_NODE_GROUP = "node_group"
        private const val KEY_STREAM_NAME = "stream_name"
        private const val KEY_USER_NAME = "username"
        private const val KEY_PASSWORD = "password"

        fun getStreamManagerHost(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_STREAM_MANAGER_HOST, "")!!
        }

        fun getStandaloneServerIp(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_STANDALONE_SERVER_IP, "")!!
        }

        fun getAppName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_APP_NAME, "live")!!
        }

        fun getNodeGroup(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_NODE_GROUP, "default")!!
        }

        fun getStreamName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_STREAM_NAME, "myStream")!!
        }

        fun getUserName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_USER_NAME, "")!!
        }

        fun getPassword(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_PASSWORD, "myStream")!!
        }
    }
}
