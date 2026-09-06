package com.splitview.pad

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Two-step picker: choose what goes in the first pane, then the second.
 *
 * The dialog opens immediately against the curated web apps and fills in the
 * installed ones as soon as the background scan lands, rather than blocking on
 * a full package query the way it used to.
 */
class AppPickerDialog(
    private val activity: Activity,
    private val onPairChosen: (AppEntry, AppEntry) -> Unit
) {

    private var stage = 1
    private var first: AppEntry? = null

    private var all: List<AppEntry> = AppCatalog.webApps
    private val visible = mutableListOf<AppEntry>()

    private lateinit var badge: TextView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var search: EditText
    private lateinit var empty: TextView
    private lateinit var backButton: ImageButton
    private val adapter = EntryAdapter()

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_app_picker, null)
        badge = view.findViewById(R.id.tv_picker_stage_badge)
        title = view.findViewById(R.id.tv_picker_title)
        subtitle = view.findViewById(R.id.tv_picker_subtitle)
        search = view.findViewById(R.id.et_app_search)
        empty = view.findViewById(R.id.tv_picker_empty)
        backButton = view.findViewById(R.id.btn_picker_back)

        val list: RecyclerView = view.findViewById(R.id.rv_installed_apps)
        list.layoutManager = LinearLayoutManager(activity)
        list.setHasFixedSize(true)
        list.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create()

        fun choose(entry: AppEntry) {
            if (stage == 1) {
                first = entry
                goToStage2()
            } else {
                val a = first
                dialog.dismiss()
                if (a != null) onPairChosen(a, entry)
            }
        }

        adapter.onClick = ::choose
        backButton.setOnClickListener { goToStage1() }
        view.findViewById<Button>(R.id.btn_add_custom_url).setOnClickListener {
            promptCustomUrl(::choose)
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) =
                filter(s?.toString().orEmpty())

            override fun afterTextChanged(s: Editable?) = Unit
        })

        goToStage1()
        dialog.show()

        // Installed apps arrive without the dialog ever having waited on them.
        AppCatalog.request(activity) { entries ->
            if (dialog.isShowing) {
                all = entries
                filter(search.text.toString())
            }
        }
    }

    private fun goToStage1() {
        stage = 1
        first = null
        badge.setText(R.string.picker_step_1)
        title.setText(R.string.picker_title_1)
        subtitle.setText(R.string.picker_subtitle_1)
        backButton.visibility = View.GONE
        search.setText("")
        filter("")
    }

    private fun goToStage2() {
        stage = 2
        badge.setText(R.string.picker_step_2)
        title.setText(R.string.picker_title_2)
        subtitle.text = activity.getString(R.string.picker_subtitle_2, first?.label.orEmpty())
        backButton.visibility = View.VISIBLE
        search.setText("")
        filter("")
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        visible.clear()
        visible += if (q.isEmpty()) {
            all
        } else {
            all.filter { it.label.lowercase().contains(q) || it.url.lowercase().contains(q) }
        }
        adapter.notifyDataSetChanged()
        empty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun promptCustomUrl(onReady: (AppEntry) -> Unit) {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.picker_custom_hint)
            setSingleLine()
            setPadding(48, 40, 48, 40)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.picker_custom_url)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val raw = input.text.toString().trim()
                if (raw.isNotEmpty()) {
                    val url = UrlUtils.normalize(raw)
                    onReady(AppEntry(UrlUtils.host(url), url))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private inner class EntryAdapter : RecyclerView.Adapter<EntryAdapter.Holder>() {

        var onClick: (AppEntry) -> Unit = {}

        inner class Holder(item: View) : RecyclerView.ViewHolder(item) {
            val icon: ImageView = item.findViewById(R.id.iv_app_icon)
            val label: TextView = item.findViewById(R.id.tv_app_label)
            val subtitle: TextView = item.findViewById(R.id.tv_app_subtitle)
            val tagView: TextView = item.findViewById(R.id.tv_app_tag)

            /** Guards against a recycled row showing the previous app's icon. */
            var boundPackage: String? = null
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_entry, parent, false)
            )

        override fun getItemCount(): Int = visible.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = visible[position]
            holder.label.text = entry.label
            holder.subtitle.text = UrlUtils.prettify(entry.url)
            holder.tagView.setText(if (entry.isInstalledApp) R.string.tag_app else R.string.tag_web)
            holder.itemView.setOnClickListener { onClick(entry) }

            holder.boundPackage = entry.packageName
            val fallback = ContextCompat.getColor(holder.icon.context, R.color.accent)
            holder.icon.setImageResource(R.drawable.ic_globe)
            holder.icon.setColorFilter(fallback)

            val pkg = entry.packageName ?: return
            AppCatalog.loadIcon(activity, pkg) { drawable ->
                if (drawable != null && holder.boundPackage == pkg) {
                    holder.icon.clearColorFilter()
                    holder.icon.setImageDrawable(drawable)
                }
            }
        }
    }
}
