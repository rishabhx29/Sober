package com.sober.system.services.sections

import android.view.accessibility.AccessibilityNodeInfo
import com.sober.core.domain.model.WindowSnapshot

/**
 * Flattens the live node tree into the plain data the rule matcher consumes.
 *
 * The walk is bounded because `TYPE_WINDOW_CONTENT_CHANGED` fires constantly on
 * scrolling feeds: an unbounded traversal on every event would burn battery in
 * the exact apps this feature targets. When the cap is reached, the snapshot is
 * simply partial — a tab selected near the root is still seen, and a missed
 * section only means no block, never a wrong one.
 */
internal object SectionSnapshotReader {

    private const val MAX_NODES = 1_500

    fun read(
        packageName: String,
        activityName: String?,
        root: AccessibilityNodeInfo
    ): WindowSnapshot {
        val resourceIds = mutableSetOf<String>()
        val selectedResourceIds = mutableSetOf<String>()
        val texts = mutableSetOf<String>()

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++

            val resourceId = node.viewIdResourceName
            if (resourceId != null) {
                resourceIds += resourceId
                if (node.isSelected) selectedResourceIds += resourceId
            }

            // `isSelected` usually lives on the tab's icon or label, not on the
            // container that owns the id, so attribute it upwards.
            if (node.isSelected) {
                var ancestor = node.parent
                var levels = 0
                while (ancestor != null && levels < 2) {
                    val ancestorId = ancestor.viewIdResourceName
                    if (ancestorId != null) {
                        selectedResourceIds += ancestorId
                        break
                    }
                    ancestor = ancestor.parent
                    levels++
                }
            }

            node.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(texts::add)
            node.contentDescription?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(texts::add)

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }

        return WindowSnapshot(
            packageName = packageName,
            activityName = activityName,
            resourceIds = resourceIds,
            selectedResourceIds = selectedResourceIds,
            texts = texts
        )
    }
}
