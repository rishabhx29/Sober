package com.sober.system.services.sections

import android.view.accessibility.AccessibilityNodeInfo
import com.sober.core.domain.usecase.SelectedSectionNode

/**
 * Finds the nodes the platform marks as selected and attributes each to the
 * resource id that owns it.
 *
 * This reads the accessibility tree, never pixels. The walk is bounded for the
 * same reason [SectionSnapshotReader] is: content events fire constantly while
 * a feed scrolls, and a recording is not worth a full-tree walk per frame.
 */
internal object SectionCaptureReader {

    private const val MAX_NODES = 1_500
    private const val MAX_ANCESTOR_LEVELS = 2
    private const val MAX_DESCENDANT_LEVELS = 2

    fun readSelected(root: AccessibilityNodeInfo): List<SelectedSectionNode> {
        val selected = LinkedHashMap<String, String?>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++

            if (node.isSelected) {
                // A tab often marks selection on its label or icon, while the id
                // lives on the container that owns both.
                val ownerId = node.viewIdResourceName ?: ancestorId(node)
                if (ownerId != null) {
                    val label = labelFor(node)
                    if (!selected.containsKey(ownerId) || selected[ownerId] == null) {
                        selected[ownerId] = label ?: selected[ownerId]
                    }
                }
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::add)
            }
        }

        return selected.map { (resourceId, label) ->
            SelectedSectionNode(resourceId = resourceId, label = label)
        }
    }

    private fun ancestorId(node: AccessibilityNodeInfo): String? {
        var ancestor = node.parent
        var levels = 0
        while (ancestor != null && levels < MAX_ANCESTOR_LEVELS) {
            ancestor.viewIdResourceName?.let { return it }
            ancestor = ancestor.parent
            levels++
        }
        return null
    }

    /**
     * A tab's label usually lives on a child icon or text view, so look down
     * before looking up. The descent stops after two levels: going deeper would
     * pick up the content of a whole selected pane instead of its tab name.
     */
    private fun labelFor(node: AccessibilityNodeInfo): String? {
        ownLabel(node)?.let { return it }
        return descendantLabel(node, depth = 0) ?: ancestorLabel(node)
    }

    private fun ownLabel(node: AccessibilityNodeInfo): String? =
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun descendantLabel(node: AccessibilityNodeInfo, depth: Int): String? {
        if (depth >= MAX_DESCENDANT_LEVELS) return null
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            ownLabel(child)?.let { return it }
            descendantLabel(child, depth + 1)?.let { return it }
        }
        return null
    }

    private fun ancestorLabel(node: AccessibilityNodeInfo): String? {
        var ancestor = node.parent
        var levels = 0
        while (ancestor != null && levels < MAX_ANCESTOR_LEVELS) {
            ownLabel(ancestor)?.let { return it }
            ancestor = ancestor.parent
            levels++
        }
        return null
    }
}
