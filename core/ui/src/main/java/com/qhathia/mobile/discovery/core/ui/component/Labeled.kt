package com.qhathia.mobile.discovery.core.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role

/**
 * Puts [description] on the touchable element itself, so screen readers and test tools (TalkBack,
 * `uiautomator`) find the label on the node that receives the tap.
 *
 * Compose moves a `contentDescription` set on a clickable element that has children into a separate
 * child node, which leaves the button itself without a label. This helper replaces the semantics of
 * the children instead, so icons and texts inside need no description of their own.
 *
 * Order matters: put it right AFTER `clickable` / `selectable` / `toggleable` on the same element. It
 * also drops the semantics of the modifiers that come after it, so the click, enabled state and
 * selection set before it are kept, and anything after it is lost. For that reason never pass it as
 * the `modifier` of a Material `IconButton` / `Button` (their click comes after the caller's
 * modifier): give an `IconButton` a plain `semantics { contentDescription = … }` with a `null` icon
 * description, and use [PrimaryActionButton] instead of a text `Button`.
 *
 * @param description localized label read for the control (e.g. "Ajustes", "Volver").
 * @param role role announced after the label; `null` keeps the one set by the clickable modifier.
 */
fun Modifier.labeled(description: String, role: Role? = Role.Button): Modifier =
    clearAndSetSemantics {
        contentDescription = description
        if (role != null) this.role = role
    }

/**
 * Label of a tappable product card: the name and the price the customer pays (the discounted one when
 * there is a discount). The list price is left out so the two amounts are not confused.
 */
internal fun cardLabel(name: String, priceText: String): String =
    "$name, $priceText"
