package net.red5.testbed

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.red5.testbed.advanced.HighQualityPublishHigherLatencyActivity
import net.red5.testbed.basic.ChatActivity
import net.red5.testbed.advanced.ConferenceActivity
import net.red5.testbed.basic.StandalonePublishActivity
import net.red5.testbed.basic.StandaloneSubscribeActivity
import net.red5.testbed.basic.StreamManagerPublishActivity
import net.red5.testbed.basic.StreamManagerSubscribeActivity

class MainActivity : AppCompatActivity() {
    private val activities: MutableList<ActivityLink> = ArrayList()
    private var recyclerView: RecyclerView? = null

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recycler_view)
        createList()
        setupRecyclerView(activities)
    }

    private fun createList() {
        addActivity(StreamManagerPublishActivity::class.java, "Publish Stream Manager(Cloud)")
        addActivity(StreamManagerSubscribeActivity::class.java, "Subscribe Stream Manager(Cloud)")
        addActivity(StandalonePublishActivity::class.java, "Publish Standalone")
        addActivity(StandaloneSubscribeActivity::class.java, "Subscribe Standalone")
        addActivity(ChatActivity::class.java, "Chat")
        addActivity(ConferenceActivity::class.java, "Conference")
        addActivity(HighQualityPublishHigherLatencyActivity::class.java, "High Quality SM Publish")

        addActivity(SettingsActivity::class.java, "Settings")
    }

    private fun addActivity(cls: Class<*>?, label: String?) {
        activities.add(ActivityLink(Intent(this, cls), label))
    }

    private fun setupRecyclerView(activities: MutableList<ActivityLink>) {
        recyclerView?.layoutManager = GridLayoutManager(this, 2)
        recyclerView?.addItemDecoration(GridSpacingItemDecoration(2, 48, true))
        recyclerView?.adapter = ButtonAdapter(activities) { position ->
            val link = activities[position]
            startActivity(link.intent)
        }
    }

    inner class ActivityLink(val intent: Intent?, val label: String?)

    inner class ButtonAdapter(
        private val links: MutableList<ActivityLink>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<ButtonAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val button: TextView = view as TextView

            init {
                button.setOnClickListener {
                    onItemClick(adapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val button = TextView(parent.context)
            button.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            button.gravity = Gravity.CENTER
            button.setPadding(16, 64, 16, 64)
            button.setTextColor(
                ResourcesCompat.getColor(
                    parent.resources,
                    R.color.textColor,
                    null
                )
            )
            button.setBackgroundColor(
                ResourcesCompat.getColor(
                    parent.resources,
                    R.color.colorPrimary,
                    null
                )
            )
            return ViewHolder(button)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.button.text = links[position].label
        }

        override fun getItemCount(): Int {
            return links.size
        }
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount

                if (position < spanCount) {
                    outRect.top = spacing
                }
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) {
                    outRect.top = spacing
                }
            }
        }
    }
}