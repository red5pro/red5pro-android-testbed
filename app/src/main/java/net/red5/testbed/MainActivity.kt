package net.red5.testbed

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import net.red5.testbed.basic.StandalonePublishActivity
import net.red5.testbed.basic.StandaloneSubscribeActivity
import net.red5.testbed.basic.StreamManagerPublishActivity
import net.red5.testbed.basic.StreamManagerSubscribeActivity

class MainActivity : AppCompatActivity(), OnItemClickListener {
    private val activities: MutableList<ActivityLink> = ArrayList<ActivityLink>()
    private var list: GridView? = null

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        list = findViewById<GridView>(R.id.list)
        createList()
        setListAdapter(activities)
    }

    private fun createList() {
        addActivity(StreamManagerPublishActivity::class.java, "Publish Stream Manager(Cloud)")
        addActivity(StreamManagerSubscribeActivity::class.java, "Subscribe Stream Manager(Cloud)")
        addActivity(StandalonePublishActivity::class.java, "Publish Standalone")
        addActivity(StandaloneSubscribeActivity::class.java, "Subscribe Standalone")


        addActivity(SettingsActivity::class.java, "Settings")
    }

    private fun addActivity(cls: Class<*>?, label: String?) {
        activities.add(ActivityLink(Intent(this, cls), label))
    }

    private fun setListAdapter(activities: MutableList<ActivityLink>) {
        list!!.setAdapter(ButtonAdapter(activities))
        list!!.setOnItemClickListener(this)
    }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
        val link = activities.get(i)
        startActivity(link.intent)
    }

    inner class ActivityLink(val intent: Intent?, val label: String?)

    inner class ButtonAdapter(private val links: MutableList<ActivityLink>) : BaseAdapter() {
        override fun getCount(): Int {
            return links.size
        }

        override fun getItem(position: Int): ActivityLink? {
            return links.get(position)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val button: TextView?
            val resources = parent.getResources()
            if (convertView == null) {
                button = TextView(parent.getContext())
                button.setLayoutParams(
                    AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                button.setGravity(Gravity.CENTER)
                button.setPadding(8, 48, 8, 48)
                button.setTextColor(ResourcesCompat.getColor(resources, R.color.textColor, null))
                button.setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.colorPrimary,
                        null
                    )
                )
                convertView = button
            } else {
                button = convertView as TextView
            }
            button.setText(links.get(position).label)
            return convertView
        }
    }
}
