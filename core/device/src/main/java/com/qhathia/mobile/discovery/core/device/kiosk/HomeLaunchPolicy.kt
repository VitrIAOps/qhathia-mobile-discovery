package com.qhathia.mobile.discovery.core.device.kiosk

/** What the app's main activity does with the intent that created it. */
internal enum class HomeLaunchAction {
    /** Continue: not a HOME intent, or the first copy of the activity on the device. */
    Proceed,

    /** A HOME intent stacked a second copy on the open one: finish it; the one below resumes. */
    FinishOnTop,

    /** A HOME intent opened a copy in its own task while another task has one: bring that back. */
    BringExistingToFront,
}

/**
 * Keeps a single main screen: with the default launchMode, every HOME intent (Home key with
 * kiosk mode off, the launcher chooser) creates another copy of the main activity. The default
 * launchMode stays because [ProcessRestartActivity] needs it.
 *
 * @param isHomeIntent the intent is `ACTION_MAIN` with `CATEGORY_HOME`.
 * @param isTaskRoot this activity is the root of its task.
 * @param otherShellTaskExists another task of the app has the same activity as its base.
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
