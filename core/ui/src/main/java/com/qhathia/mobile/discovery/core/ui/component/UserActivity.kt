package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * Reports one user interaction to the app shell, which restarts the customer-session inactivity
 * countdown (design D3). Provided by the shell; defaults to a no-op for previews and tests.
 *
 * Dialogs are separate windows and the soft keyboard sends no pointer events, so the shell's touch
 * observer on the main window cannot see them. Dialog roots apply [reportUserActivity], and the shared
 * text fields call this from `onValueChange`.
 */
val LocalUserActivity = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Reports every touch-down inside this element to [LocalUserActivity]. Observes the Initial pass and
 * never consumes, so taps and scrolls still reach the content. Apply it to the content root of every
 * app dialog.
 */
fun Modifier.reportUserActivity(): Modifier = this then ReportUserActivityElement

private data object ReportUserActivityElement : ModifierNodeElement<ReportUserActivityNode>() {
    override fun create() = ReportUserActivityNode()
    override fun update(node: ReportUserActivityNode) = Unit
    override fun InspectorInfo.inspectableProperties() {
        name = "reportUserActivity"
    }
}

private class ReportUserActivityNode :
    Modifier.Node(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass == PointerEventPass.Initial && pointerEvent.changes.any(PointerInputChange::changedToDownIgnoreConsumed)) {
            currentValueOf(LocalUserActivity).invoke()
        }
    }

    override fun onCancelPointerInput() = Unit
}
