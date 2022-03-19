package com.designdrivendevelopment.kotelok.screens.trainers.swipe

import android.animation.ValueAnimator
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import androidx.annotation.Px
import androidx.core.animation.doOnEnd
import androidx.core.util.isNotEmpty
import androidx.core.util.size
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
class SwipeLayoutManager(
    private val onSwipeLeft: (pos: Int) -> Unit,
    private val onSwipeRight: (pos: Int) -> Unit
) : RecyclerView.LayoutManager() {
    private var stackTopPos = 0

    private val shownItemsCount = DEFAULT_SHOWN_ITEMS_COUNT
    private val elevationStep = DEFAULT_ELEVATION_STEP
    private val itemsSizeRatio = DEFAULT_ITEMS_SIZE_RATIO
    private val relativeStackHeight = DEFAULT_RELATIVE_STACK_HEIGHT
    private val scales = List(size = shownItemsCount) { index: Int -> itemsSizeRatio.pow(index) }

    private val relativeSwipeThreshold = DEFAULT_RELATIVE_SWIPE_THRESHOLD
    private var rightSwipeThreshold = 0
    private var leftSwipeThreshold = 0

    private var absoluteStackHeight = 0
    private var stackBottom = 0
    private var baseOffset = 0f

    private var topView: View? = null
    private var anchorView: View? = null

    private val verticalThresholds: MutableList<Int> = MutableList(shownItemsCount) { 0 }
    private val verticalOffsets: MutableList<Int> = MutableList(shownItemsCount - 1) { 0 }

    private val viewCache = SparseArray<View>()
    private val saveTopItemOnChanges = false

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (recycler == null) return
        rightSwipeThreshold = (width * (1 - relativeSwipeThreshold)).roundToInt()
        leftSwipeThreshold = (width * relativeSwipeThreshold).roundToInt()

        val isPreLayout = state?.isPreLayout == true
        if (isPreLayout) {
            for (childPos in childCount - 1 downTo 0) {
                val view = getChildAt(childPos) ?: continue
                viewCache.append(childPos, view)

                /*
                * Поиск якорной view для сохранения местоположения верхнего элемента
                * при изменениях данных в адаптере
                */
                if (saveTopItemOnChanges && anchorView != null) {
                    testToAnchor(view)
                }
            }
        } else if (viewCache.isNotEmpty() && saveTopItemOnChanges) {
            stackTopPos = anchorView?.let { getPosition(it) } ?: stackTopPos
            anchorView = null
        }

        if (stackTopPos !in 0 until itemCount) {
            stackTopPos = 0
        }

        detachAndScrapAttachedViews(recycler)
        fill(recycler, isPreLayout, remeasureStack = true)
        recycler.clear()
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val view = topView
        if (view == null || recycler == null) return 0

        return when {
            (dx < 0) && ((view.centerX - dx) > rightSwipeThreshold) -> {
                topView = null

                val viewPos = getPosition(view)
                val delta = max(rightSwipeThreshold - view.centerX, 0)
                if (delta != 0) {
                    scrollStackItemsVertically(width / 2, view.centerX, delta)
                }

                view.moveAlongAxis(
                    startPos = view.left,
                    endPos = view.left + view.width,
                    SWIPE_ON_EDGE_DURATION,
                    onUpdate = { updateViewRotation(view, width / 2) },
                    onEnd = {
                        stackTopPos ++
                        detachAndScrapAttachedViews(recycler)
                        fill(recycler)
                        recycler.clear()
                        onSwipeRight.invoke(viewPos)
                    }
                )
                0
            }

            (dx > 0) && ((view.centerX - dx) < leftSwipeThreshold) -> {
                topView = null

                val viewPos = getPosition(view)
                val delta = - max(view.centerX - leftSwipeThreshold, 0)
                if (delta != 0) {
                    scrollStackItemsVertically(width / 2, view.centerX, delta)
                }

                view.moveAlongAxis(
                    startPos = view.left,
                    endPos = view.left - view.width,
                    SWIPE_ON_EDGE_DURATION,
                    onUpdate = { updateViewRotation(view, width / 2) },
                    onEnd = {
                        stackTopPos ++
                        onSwipeLeft.invoke(viewPos)
                        detachAndScrapAttachedViews(recycler)
                        fill(recycler)
                        recycler.clear()
                    }
                )
                0
            }
            else -> {
                view.offsetLeftAndRight(-dx)
                updateViewRotation(view, width / 2)
                scrollStackItemsVertically(width / 2, view.centerX, -dx)
                dx
            }
        }
    }

    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            val view = topView ?: return

            if ((view.centerX <= rightSwipeThreshold) && (view.centerX >= leftSwipeThreshold)) {
                view.moveAlongAxis(
                    view.left,
                    view.marginStart,
                    RECOVERY_DURATION,
                    onUpdate = { step ->
                        val delta = - step
                        scrollStackItemsVertically(width / 2, view.centerX, delta)
                        updateViewRotation(view, width / 2)
                    }
                )
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable = SavedState(stackTopPos)

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            stackTopPos = state.stackTopPos
        }
    }

    private fun fill(
        recycler: RecyclerView.Recycler,
        isPreLayout: Boolean = false,
        remeasureStack: Boolean = false,
    ) {
        if (itemCount == 0 || (stackTopPos >= itemCount)) return

        if (isPreLayout) {
            for (viewPos in (viewCache.size - 1) downTo 0) {
                val view = viewCache.get(viewPos)
                layoutViewOnPos(view, viewPos, viewCache.size - 1, posShift = 0, remeasureStack)
                updateViewDecor(view)
            }
        } else {
            val visibleViewsCount = min(itemCount - stackTopPos, shownItemsCount)
            val bottomViewPos = visibleViewsCount - 1 + stackTopPos

            for (viewPos in bottomViewPos downTo stackTopPos) {
                val view = recycler.getViewForPosition(viewPos)
                layoutViewOnPos(view, viewPos, bottomViewPos, posShift = stackTopPos, remeasureStack)
                updateViewDecor(view)

                if (viewPos == stackTopPos) {
                    topView = view
                }
            }

            if (viewCache.isNotEmpty() && recycler.scrapList.isNotEmpty()) {
                val disappearingIndexes = findDisappearingIndexes(recycler.scrapList)
                layoutDisappearingViews(disappearingIndexes)
                viewCache.clear()
            }
        }
    }

    private fun updateViewScale(view: View, @Px verticalSpace: Int, @Px bottomThreshold: Int) {
        val dy = min(verticalSpace, bottomThreshold - view.bottom)

        val endScale = scales.last()
        val scaleCoefficient = 1 - endScale

        val scale: Float = endScale + scaleCoefficient * (dy.toFloat() / verticalSpace.toFloat())
        view.scaleX = scale
        view.scaleY = scale
    }

    private fun updateViewElevation(view: View, @Px verticalSpace: Int, @Px bottomThreshold: Int) {
        val dy = min(verticalSpace, max(bottomThreshold - view.bottom, 0))

        val elevation = elevationStep * (1 + shownItemsCount * (dy.toFloat() / verticalSpace.toFloat()))
        view.elevation = elevation
    }

    private fun updateViewRotation(view: View, @Px origin: Int) {
        val delta = view.centerX - origin
        when {
            delta == 0 -> view.rotation = 0f
            delta > 0 -> {
                val ratio = delta.toFloat() / (rightSwipeThreshold - origin)
                view.rotation = MAX_ROTATION_ANGLE * ratio
            }

            delta < 0 -> {
                val ratio = delta.toFloat() / (origin - leftSwipeThreshold)
                view.rotation = MAX_ROTATION_ANGLE * ratio
            }
        }
    }

    private fun updateViewDecor(view: View) {
        view.pivotY = view.height.toFloat()
        view.pivotX = view.width.toFloat() / 2
        updateViewScale(view, absoluteStackHeight, stackBottom)
        updateViewElevation(view, absoluteStackHeight, stackBottom)
    }

    private fun View.moveAlongAxis(
        startPos: Int,
        endPos: Int,
        moveDuration: Long,
        onUpdate: ((Int) -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        var prevValue = startPos
        ValueAnimator.ofInt(startPos, endPos).apply {
            duration = moveDuration
            addUpdateListener {
                val step = (it.animatedValue as Int) - prevValue
                this@moveAlongAxis.offsetLeftAndRight(step)
                prevValue = it.animatedValue as Int
                onUpdate?.invoke(step)
            }
            doOnEnd { onEnd?.invoke() }
        }.start()
    }

    private fun scrollStackItemsVertically(@Px origin: Int, @Px startPos: Int, @Px delta: Int) {
        when {
            (delta == 0) -> return
            (delta < 0) -> {
                // Сдвиг влево
                when {
                    (startPos < origin) -> {
                        // Подвинуть вьюшки вверх
                        val percent = (origin - startPos - delta).toFloat() / (origin - leftSwipeThreshold)
                        moveStackItemsToPercent(percent)
                    }

                    (startPos + delta > origin) -> {
                        // Подвинуть вьюхи вниз
                        val percent = (startPos - origin + delta).toFloat() / (rightSwipeThreshold - origin)
                        moveStackItemsToPercent(percent)
                    }

                    else -> {
                        // Подвинуть вниз, к началу
                        val percentBottom = 0f
                        moveStackItemsToPercent(percentBottom)

                        // Подвинуть вверх на - delta - startPos + origin
                        val percentTop = (- delta - (startPos - origin)).toFloat() / (origin - leftSwipeThreshold)
                        moveStackItemsToPercent(percentTop)
                    }
                }
            }

            (delta > 0) -> {
                // Свдиг вправо
                when {
                    (startPos > origin) -> {
                        // Подвинуть вьюшки вверх
                        val percent = (startPos - origin + delta).toFloat() / (rightSwipeThreshold - origin)
                        moveStackItemsToPercent(percent)
                    }

                    (startPos + delta < origin) -> {
                        // Подвинуть вьюшки вниз
                        val percent = (origin - startPos + delta).toFloat() / (origin - leftSwipeThreshold)
                        moveStackItemsToPercent(percent)
                    }

                    else -> {
                        // Подвинуть вниз, к началу
                        val percentBottom = 0f
                        moveStackItemsToPercent(percentBottom)

                        // Подвинуть вверх на delta - (origin - startPos)
                        val percentTop = (delta - (origin - startPos)).toFloat() / (origin - leftSwipeThreshold)
                        moveStackItemsToPercent(percentTop)
                    }
                }
            }
        }
    }

    private fun remeasureStackParams(itemHeight: Int) {
        absoluteStackHeight = ((height / 2 - itemHeight / 2) * relativeStackHeight).roundToInt()
        stackBottom = (height / 2 + itemHeight / 2 + absoluteStackHeight)
        baseOffset = absoluteStackHeight / scales.dropLast(1).sum()

        verticalThresholds[0] = stackBottom - absoluteStackHeight
        verticalThresholds[shownItemsCount - 1] = stackBottom
        for (i in (shownItemsCount - 2) downTo 1) {
            val offset = (baseOffset * scales[i]).roundToInt()
            verticalOffsets[shownItemsCount - 2 - i] = offset
            verticalThresholds[i] = verticalThresholds[i + 1] - offset
        }
        verticalOffsets[shownItemsCount - 2] = verticalThresholds[1] - verticalThresholds[0]
    }

    private fun moveStackItemsToPercent(percent: Float) {
        for (childPos in 0 until childCount - 1) {
            val view = getChildAt(childPos)
            if (view != null) {
                val newCoordinate = if (percent >= 1f) {
                    verticalThresholds[childCount - 2 - childPos]
                } else {
                    verticalThresholds[childCount - 1 - childPos] - (verticalOffsets[childPos] * percent).roundToInt()
                }

                val offset = newCoordinate - view.bottom
                view.offsetTopAndBottom(offset)
                updateViewScale(view, absoluteStackHeight, stackBottom)
                updateViewElevation(view, absoluteStackHeight, stackBottom)
            }
        }
    }

    private fun layoutViewOnPos(
        view: View,
        pos: Int,
        bottomPos: Int,
        posShift: Int,
        remeasureStack: Boolean
    ) {
        addView(view)
        measureChildWithMargins(view, 0, 0)

        if (pos == bottomPos && remeasureStack) {
            remeasureStackParams(view.measuredHeight)
        }

        layoutDecorated(
            view,
            view.marginStart,
            verticalThresholds[pos - posShift] - view.measuredHeight,
            width - view.marginEnd,
            verticalThresholds[pos - posShift]
        )
    }

    private fun findDisappearingIndexes(scrapList: List<RecyclerView.ViewHolder?>): List<Int> {
        val disappearingIndexes = mutableListOf<Int>()

        for (viewHolder in scrapList.iterator()) {
            if (viewHolder == null) continue
            val index = viewCache.indexOfValue(viewHolder.itemView)
            if (index != -1) disappearingIndexes.add(index)
        }
        return disappearingIndexes
    }

    private fun layoutDisappearingViews(disappearingIndexes: List<Int>) {
        for (index in disappearingIndexes) {
            val viewPos = viewCache.keyAt(index)
            val view = viewCache.valueAt(index)

            addDisappearingView(view)
            measureChildWithMargins(view, 0, 0)

            layoutDecorated(
                view,
                width + view.marginStart + view.width,
                verticalThresholds[viewPos] - view.measuredHeight,
                width + view.marginStart + view.width + view.width,
                verticalThresholds[viewPos]
            )

            updateViewDecor(view)
        }
    }

    private fun testToAnchor(view: View) {
        val lp = view.layoutParams as RecyclerView.LayoutParams
        if (!lp.isItemRemoved) {
            anchorView = view
        }
    }

    private class SavedState(val stackTopPos: Int) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readInt())

        override fun writeToParcel(parcel: Parcel?, flags: Int) {
            parcel?.writeInt(stackTopPos)
        }

        override fun describeContents(): Int = 0

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState = SavedState(parcel)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    private val View.centerX: Int
        get() = this.left + this.width / 2

    companion object {
        private const val MAX_ROTATION_ANGLE = 5f
        private const val RECOVERY_DURATION = 200L
        private const val SWIPE_ON_EDGE_DURATION = 300L

        private const val DEFAULT_SHOWN_ITEMS_COUNT = 5
        private const val DEFAULT_ELEVATION_STEP = 5f
        private const val DEFAULT_ITEMS_SIZE_RATIO = 0.93f
        private const val DEFAULT_RELATIVE_STACK_HEIGHT = 4 / 12f
        private const val DEFAULT_RELATIVE_SWIPE_THRESHOLD = 1 / 6f
    }
}
