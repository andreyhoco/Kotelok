package com.designdrivendevelopment.kotelok.screens.trainers.cards

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart

/*
 * 29.05.2022
 * Позднее надо нормально кастомизировать этот класс - настройка длительности и т. д.
 */
class FlashCardAnimations {
    private val flipAnimations: MutableList<Animator> = mutableListOf()

    fun flipCard(startView: View, endView: View) {
        val flipInAnimation = ObjectAnimator
            .ofFloat(
                startView,
                View.ROTATION_X,
                FLIP_START_POS,
                FLIP_END_POS
            ).apply {
                duration = FLIP_DURATION / 2
            }
        val flipOutAnimation = ObjectAnimator
            .ofFloat(
                endView,
                View.ROTATION_X,
                - FLIP_END_POS,
                FLIP_START_POS
            ).apply {
                duration = FLIP_DURATION / 2
                doOnStart {
                    startView.visibility = View.INVISIBLE
                    endView.visibility = View.VISIBLE
                }
            }
        flipAnimations.add(flipOutAnimation)
        flipAnimations.add(flipInAnimation)
        flipInAnimation.start()
        flipOutAnimation.start()
    }

    fun flipCardContainer(context: Context, container: View) {
        val scale = context.resources.displayMetrics.density
        val cameraDist = CAMERA_DIST_SCALE * scale
        container.cameraDistance = cameraDist

        val containerFlipIn = ObjectAnimator.ofFloat(container, View.ROTATION_X, FLIP_START_POS, FLIP_END_POS)
        val containerFlipOut = ObjectAnimator
            .ofFloat(
                container,
                View.ROTATION_X,
                - FLIP_END_POS,
                FLIP_START_POS
            ).apply {
                duration = FLIP_DURATION / 2
            }
        flipAnimations.add(containerFlipIn)
        flipAnimations.add(containerFlipOut)
        containerFlipIn.apply {
            duration = FLIP_DURATION / 2
            doOnEnd {
                container.rotationX = - FLIP_END_POS
                containerFlipOut.start()
            }
            start()
        }
    }

    fun endAnimations() {
        flipAnimations.forEach { it.end() }
        flipAnimations.clear()
    }

    companion object {
        const val FLIP_DURATION = 350L
        const val CAMERA_DIST_SCALE = 8000
        const val FLIP_START_POS = 0f
        const val FLIP_END_POS = 89f
    }
}
