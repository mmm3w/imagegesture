package com.mitsuki.armory.imagegesture

import android.view.animation.Animation

internal fun Animation?.isAnimationRunning(): Boolean {
    if (this == null) return false
    return hasStarted() && !hasEnded()
}