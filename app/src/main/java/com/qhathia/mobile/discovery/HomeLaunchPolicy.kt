package com.qhathia.mobile.discovery

/** What [MainActivity] does with the intent that created it (fix-cart-inactivity-hidden-activity). */
internal enum class HomeLaunchAction {
    /** Show the showcase: not a HOME intent, or the first showcase on the device. */
    Proceed,

    /** A HOME intent stacked a second showcase on the open one: finish it; the one below resumes. */
    FinishOnTop,

    /** A HOME intent opened a showcase in its own task while another task has one: bring that back. */
    BringExistingToFront,
}

/**
 * Keeps a single showcase screen (F-35): with the default launchMode, every HOME intent (Home key with
 * kiosk mode off, the launcher chooser) creates another [MainActivity], whose timer would end the
 * customer session of the visible one. The default launchMode stays: the RESTART_APP trampoline needs it.
 *
 * @param isHomeIntent the intent is `ACTION_MAIN` with `CATEGORY_HOME`.
 * @param isTaskRoot this activity is the root of its task.
 * @param otherShellTaskExists another task of the app has [MainActivity] as its base.
 */
internal fun homeLaunchAction(
    isHomeIntent: Boolean,
    isTaskRoot: Boolean,
    otherShellTaskExists: Boolean,
): HomeLaunchAction = when {
    !isHomeIntent -> HomeLaunchAction.Proceed
    !isTaskRoot -> HomeLaunchAction.FinishOnTop
    otherShellTaskExists -> HomeLaunchAction.BringExistingToFront
    else -> HomeLaunchAction.Proceed
}
