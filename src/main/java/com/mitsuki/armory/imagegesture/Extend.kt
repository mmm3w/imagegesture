package com.mitsuki.armory.imagegesture

import android.view.animation.Animation

fun Animation?.isAnimationRunning(): Boolean {
    if (this == null) return false
    return hasStarted() && !hasEnded()
}