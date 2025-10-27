package net.red5.testbed

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Xml
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.red5.android.BuildConfig
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import kotlin.io.path.name

class SettingsActivity : AppCompatActivity() {
    private var etLicenseKey: EditText? = null
    private var etStreamManagerHost: EditText? = null
    private var etStandaloneServerIp: EditText? = null
    private var etAppName: EditText? = null
    private var etNodeGroup: EditText? = null
    private var etStreamName: EditText? = null
    private var etUserName: EditText? = null
    private var etPassword: EditText? = null
    private var rgDtlsSetup: RadioGroup? = null
    private var rbActpass: RadioButton? = null
    private var rbActive: RadioButton? = null
    private var rbPassive: RadioButton? = null

    private var btnSave: Button? = null

    private var sharedPreferences: SharedPreferences? = null

    private var defaultLicenseKey: String? = ""
    private var defaultStreamManagerEndpoint: String? = ""
    private var defaultStandaloneEndpoint: String? = ""

    private fun valueOr(value: String?, defaultValue: String): String {
        return if (value == null || value == "N/A") defaultValue else value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val config = loadTestConfig()
        defaultLicenseKey = valueOr(config["license_key"], "")
        defaultStreamManagerEndpoint = valueOr(config["sm_endpoint"], "")
        defaultStandaloneEndpoint = valueOr(config["standalone_endpoint"], "")

        initViews()
        loadSettings()

        btnSave!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                saveSettings()
            }
        })
    }

    private fun loadTestConfig(): Map<String, String> {
        val config = mutableMapOf<String, String>()
        val inputStream: InputStream = resources.openRawResource(R.raw.test)

        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "entry") {
                    // It's an <entry> tag, so we grab the 'key' and 'value' attributes.
                    val key = parser.getAttributeValue(null, "key")
                    val value = parser.getAttributeValue(null, "value")

                    if (key != null && value != null) {
                        // Add the found key-value pair to our map.
                        config[key] = value
                    }
                }
                eventType = parser.next() // Move to the next XML event.
            }

        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream.close()
            return config
        }
    }

    private fun initViews() {
        etLicenseKey = findViewById<EditText>(R.id.et_license_key)
        etStreamManagerHost = findViewById<EditText>(R.id.et_stream_manager_host)
        etStandaloneServerIp = findViewById<EditText>(R.id.et_standalone_server_ip)
        etAppName = findViewById<EditText>(R.id.et_app_name)
        etNodeGroup = findViewById<EditText>(R.id.et_node_group)
        etStreamName = findViewById<EditText>(R.id.et_stream_name)
        etUserName = findViewById<EditText>(R.id.et_username)
        etPassword = findViewById<EditText>(R.id.et_password)
        btnSave = findViewById<Button>(R.id.btn_save)
        rgDtlsSetup = findViewById<RadioGroup>(R.id.rg_dtls_setup)
        rbActpass = findViewById<RadioButton>(R.id.rb_actpass)
        rbActive = findViewById<RadioButton>(R.id.rb_active)
        rbPassive = findViewById<RadioButton>(R.id.rb_passive)
    }

    private fun loadSettings() {
        var licenseKey: String = sharedPreferences!!.getString(KEY_LICENSE_KEY,
            defaultLicenseKey)!!
        if (licenseKey.isEmpty()) {
            licenseKey = defaultLicenseKey!!
        }
        var streamManagerHost: String = sharedPreferences!!.getString(KEY_STREAM_MANAGER_HOST,
            defaultStreamManagerEndpoint)!!
        if (streamManagerHost.isEmpty()) {
            streamManagerHost = defaultStreamManagerEndpoint!!
        }
        var standaloneServerIp: String =
            sharedPreferences!!.getString(KEY_STANDALONE_SERVER_IP, defaultStandaloneEndpoint)!!
        if (standaloneServerIp.isEmpty()) {
            standaloneServerIp = defaultStandaloneEndpoint!!
        }
        val appName: String = sharedPreferences!!.getString(KEY_APP_NAME, "live")!!
        val nodeGroup: String = sharedPreferences!!.getString(KEY_NODE_GROUP, "default")!!
        val streamName: String = sharedPreferences!!.getString(KEY_STREAM_NAME, "myStream")!!
        val username: String = sharedPreferences!!.getString(KEY_USER_NAME, "")!!
        val password: String = sharedPreferences!!.getString(KEY_PASSWORD, "")!!
        val dtlsSetup: String = sharedPreferences!!.getString(KEY_DTLS_SETUP, "actpass")!!

        etLicenseKey!!.setText(licenseKey)
        etStreamManagerHost!!.setText(streamManagerHost)
        etStandaloneServerIp!!.setText(standaloneServerIp)
        etAppName!!.setText(appName)
        etNodeGroup!!.setText(nodeGroup)
        etStreamName!!.setText(streamName)
        etUserName!!.setText(username)
        etPassword!!.setText(password)


        when (dtlsSetup) {
            "active" -> rbActive!!.isChecked = true
            "passive" -> rbPassive!!.isChecked = true
            "actpass" -> rbActpass!!.isChecked = true
            else -> rbActpass!!.isChecked = true
        }
    }

    private fun saveSettings() {
        val licenseKey = etLicenseKey!!.getText().toString().trim { it <= ' ' }
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

        var dtlsSetup = "actpass" // default
        val selectedId = rgDtlsSetup!!.checkedRadioButtonId
        if (selectedId == R.id.rb_active) {
            dtlsSetup = "active"
        } else if (selectedId == R.id.rb_passive) {
            dtlsSetup = "passive"
        } else if (selectedId == R.id.rb_actpass) {
            dtlsSetup = "actpass"
        }

        val editor = sharedPreferences!!.edit()
        editor.putString(KEY_LICENSE_KEY, licenseKey)
        editor.putString(KEY_STREAM_MANAGER_HOST, streamManagerHost)
        editor.putString(KEY_STANDALONE_SERVER_IP, standaloneServerIp)
        editor.putString(KEY_APP_NAME, appName)
        editor.putString(KEY_NODE_GROUP, nodeGroup)
        editor.putString(KEY_STREAM_NAME, streamName)
        editor.putString(KEY_USER_NAME, userName)
        editor.putString(KEY_PASSWORD, password)


        editor.putString(KEY_DTLS_SETUP, dtlsSetup)

        editor.apply()

        Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveSettings()
    }

    companion object {
        private const val PREFS_NAME = "Red5ProSettings"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_STREAM_MANAGER_HOST = "stream_manager_host"
        private const val KEY_STANDALONE_SERVER_IP = "standalone_server_ip"
        private const val KEY_APP_NAME = "app_name"
        private const val KEY_NODE_GROUP = "node_group"
        private const val KEY_STREAM_NAME = "stream_name"
        private const val KEY_USER_NAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ENABLE_DEBUG = "enable_debug"
        private const val KEY_DTLS_SETUP = "dtls_setup"

        fun getLicenseKey(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_LICENSE_KEY, "")!!
        }

        fun getStreamManagerHost(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_STREAM_MANAGER_HOST, "")!!
        }

        fun isDebugEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLE_DEBUG, false)
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

        fun getDtlsSetup(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            return prefs.getString(KEY_DTLS_SETUP, "actpass")!!
        }
    }
}