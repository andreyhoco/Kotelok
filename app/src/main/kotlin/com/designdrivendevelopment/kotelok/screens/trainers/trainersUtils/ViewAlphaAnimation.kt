package com.designdrivendevelopment.kotelok.screens.trainers.trainersUtils

import android.animation.ObjectAnimator
import android.view.View
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible

class ViewAlphaAnimation {
    private val appearanceAnimations: MutableList<ObjectAnimator> = mutableListOf()
    private val fadeAnimations: MutableList<ObjectAnimator> = mutableListOf()

    fun startAppearance(target: View?, animDuration: Long, onEnd: (() -> Unit)? = null) {
        if (target == null) return
        val anim = ObjectAnimator.ofFloat(
            target, View.ALPHA,
            ALPHA_TRANSPARENT,
            ALPHA_VISIBLE
        ).apply {
            duration = animDuration
            doOnStart { target.isVisible = true }
            doOnCancel { onEnd?.invoke() }
            doOnEnd { onEnd?.invoke() }
        }
        appearanceAnimations.add(anim)
        anim.start()
    }

    fun startFade(target: View?, animDuration: Long, onEnd: (() -> Unit)? = null) {
        if (target == null) return
        val anim = ObjectAnimator.ofFloat(
            target, View.ALPHA,
            ALPHA_VISIBLE,
            ALPHA_TRANSPARENT
        ).apply {
            duration = animDuration
            doOnCancel {
                target.isVisible = false
                onEnd?.invoke()
            }
            doOnEnd {
                target.isVisible = false
                onEnd?.invoke()
            }
        }
        fadeAnimations.add(anim)
        anim.start()
    }

    fun endAppearanceAnimations() {
        appearanceAnimations.forEach { it.end() }
        appearanceAnimations.clear()
    }

    fun endFadeAnimations() {
        fadeAnimations.forEach { it.end() }
        fadeAnimations.clear()
    }

    fun cancelAppearanceAnimations() {
        appearanceAnimations.forEach { it.cancel() }
        appearanceAnimations.clear()
    }

    fun cancelFadeAnimations() {
        fadeAnimations.forEach { it.cancel() }
        fadeAnimations.clear()
    }

    companion object {
        private const val ALPHA_TRANSPARENT = 0f
        private const val ALPHA_VISIBLE = 1f
    }
}
