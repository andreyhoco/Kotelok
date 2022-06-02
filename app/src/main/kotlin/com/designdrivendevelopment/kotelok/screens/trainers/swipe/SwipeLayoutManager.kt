package com.designdrivendevelopment.kotelok.screens.trainers.swipe

import android.animation.ValueAnimator
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
    private val onSwipeRight: (pos: Int) -> Unit,
    private var shownItemsCount: Int = DEFAULT_SHOWN_ITEMS_COUNT,
    private var itemsSizeRatio: Float = DEFAULT_ITEMS_SIZE_RATIO,
) : RecyclerView.LayoutManager() {
    private constructor(config: Config) :
        this(
            config.onSwipeLeft,
            config.onSwipeRight,
            config.shownItemsCount,
            config.itemsSizeRatio
        ) {
            elevationStep = config.elevationStep
            relativeStackHeight = config.relativeStackHeight
            relativeSwipeThreshold = config.relativeSwipeThreshold
            saveTopItemOnChanges = config.saveTopOnItemChanges
            pivotYType = config.pivotY
        }

    var stackTopPos = 0
        private set

    private var elevationStep = DEFAULT_ELEVATION_STEP
    private val scales = List(size = shownItemsCount) { index: Int -> itemsSizeRatio.pow(index) }

    private val endScale = scales.last()
    private val offsetScalesSum = scales.dropLast(1).sum()

    private var relativeStackHeight = DEFAULT_RELATIVE_STACK_HEIGHT
    private var relativeSwipeThreshold = DEFAULT_RELATIVE_SWIPE_THRESHOLD
    private var rightSwipeThreshold = 0
    private var leftSwipeThreshold = 0

    private var absoluteStackHeight = 0
    private var stackBottom = 0
    private var baseOffset = 0f
    private var pivotYType = Config.Pivot.BOTTOM

    private var topView: View? = null
    private var anchorView: View? = null

    private val verticalThresholds: MutableList<Int> = MutableList(shownItemsCount) { 0 }
    private val verticalOffsets: MutableList<Int> = MutableList(shownItemsCount - 1) { 0 }

    private val viewCache = SparseArray<View>()
    private var saveTopItemOnChanges = false

    private val appearanceAnimators = mutableListOf<ValueAnimator>()

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
                if (saveTopItemOnChanges && (anchorView == null)) {
                    testToAnchor(view)
                }
            }
        } else if (viewCache.isNotEmpty() && saveTopItemOnChanges) {
            stackTopPos = anchorView?.let { getPosition(it) } ?: stackTopPos
            anchorView = null
        }

        detachAndScrapAttachedViews(recycler)
        fill(recycler, isPreLayout, remeasureStack = true)
        if (!isPreLayout) recycler.clear()
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    override fun scrollToPosition(position: Int) {
        stackTopPos = position
        requestLayout()
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val view = topView
        if (view == null || recycler == null) return 0

        val origin = width shr 1
        return when {
            (dx < 0) && ((view.centerX - dx) > rightSwipeThreshold) -> {
                topView = null

                val viewPos = getPosition(view)
                val delta = max(rightSwipeThreshold - view.centerX, 0)
                if (delta != 0) {
                    scrollStackItemsVertically(origin, view.centerX, delta)
                }

                view.moveAlongAxis(
                    startPos = view.left,
                    endPos = view.left + view.width,
                    SWIPE_ON_EDGE_DURATION,
                    onUpdate = { updateViewRotation(view, origin) },
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
                    scrollStackItemsVertically(origin, view.centerX, delta)
                }

                view.moveAlongAxis(
                    startPos = view.left,
                    endPos = view.left - view.width,
                    SWIPE_ON_EDGE_DURATION,
                    onUpdate = { updateViewRotation(view, origin) },
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
                updateViewRotation(view, origin)
                scrollStackItemsVertically(origin, view.centerX, -dx)
                dx
            }
        }
    }

    override fun onScrollStateChanged(state: Int) {
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            val view = topView ?: return
            val origin = width shr 1

            if ((view.centerX <= rightSwipeThreshold) && (view.centerX >= leftSwipeThreshold)) {
                view.moveAlongAxis(
                    view.left,
                    view.marginStart,
                    RECOVERY_DURATION,
                    onUpdate = { step ->
                        val delta = - step
                        scrollStackItemsVertically(origin, view.centerX, delta)
                        updateViewRotation(view, origin)
                    }
                )
            }
        }
    }

    private fun fill(
        recycler: RecyclerView.Recycler,
        isPreLayout: Boolean = false,
        remeasureStack: Boolean = false,
    ) {
        appearanceAnimators.forEach { it.end() }
        appearanceAnimators.clear()

        if (itemCount == 0 || (stackTopPos !in 0 until itemCount)) return

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

                /*
                * (viewPos == bottomViewPos) - view нижняя в "стопке"
                * (visibleViewsCount == shownItemsCount) - "стопка" имеет макс. размер, т. о.
                * появляется новая нижняя view, а не имеет мест оперемещение старой view снизу вверх
                * (viewPos != (shownItemsCount - 1)) - определяет, что lm не находится в начальном состоянии
                */
                if ((viewPos == bottomViewPos) &&
                    (visibleViewsCount == shownItemsCount) &&
                    (viewPos != (shownItemsCount - 1))
                ) {
                    appearanceAnimators.add(startViewAppearance(view, APPEARANCE_DURATION))
                }

                if (viewPos == stackTopPos) {
                    topView = view
                }
            }

            if (viewCache.isNotEmpty()) {
                if (recycler.scrapList.isNotEmpty()) {
                    val disappearingIndexes = findDisappearingIndexes(recycler.scrapList)
                    layoutDisappearingViews(disappearingIndexes)
                }
                viewCache.clear()
            }
        }
    }

    private fun updateViewScale(view: View, @Px verticalSpace: Int, @Px bottomThreshold: Int) {
        val dy = min(verticalSpace, bottomThreshold - view.bottom)
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
        view.pivotY = when (pivotYType) {
            Config.Pivot.BOTTOM -> view.height.toFloat()
            Config.Pivot.CENTER -> (view.height.toFloat() / 2)
            else -> 0f
        }
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

    private fun startViewAppearance(view: View, appearanceDuration: Long): ValueAnimator {
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = appearanceDuration
            addUpdateListener { animator ->
                view.alpha = animator.animatedValue as Float
            }
            start()
        }
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
        absoluteStackHeight = (((height shr 1) - (itemHeight shr 1)) * relativeStackHeight).roundToInt()
        stackBottom = ((height shr 1) + (itemHeight shr 1) + absoluteStackHeight)
        baseOffset = absoluteStackHeight / offsetScalesSum

        verticalThresholds[0] = stackBottom - absoluteStackHeight
        verticalThresholds[shownItemsCount - 1] = stackBottom
        for (i in (shownItemsCount - 2) downTo 1) {
            val offset = (baseOffset * scales[i]).roundToInt()
            verticalOffsets[i] = offset
            verticalThresholds[i] = verticalThresholds[i + 1] - offset
        }
        verticalOffsets[0] = verticalThresholds[1] - verticalThresholds[0]
    }

    private fun moveStackItemsToPercent(percent: Float) {
        for (childPos in 0 until childCount - 1) {
            val view = getChildAt(childPos)
            if (view != null) {
                val newCoordinate = if (percent >= 1f) {
                    verticalThresholds[childCount - 2 - childPos]
                } else {
                    verticalThresholds[childCount - 1 - childPos] -
                        (verticalOffsets[childCount - 2 - childPos] * percent).roundToInt()
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

    private val View.centerX: Int
        get() = this.left + (this.width shr 1)

    class Config {
        enum class Pivot {
            TOP,
            CENTER,
            BOTTOM
        }

        var shownItemsCount = DEFAULT_SHOWN_ITEMS_COUNT
            private set
        var itemsSizeRatio = DEFAULT_ITEMS_SIZE_RATIO
            private set
        var elevationStep = DEFAULT_ELEVATION_STEP
            private set
        var relativeStackHeight = DEFAULT_RELATIVE_STACK_HEIGHT
            private set
        var relativeSwipeThreshold = DEFAULT_RELATIVE_SWIPE_THRESHOLD
            private set
        var saveTopOnItemChanges = false
            private set
        var onSwipeLeft: (pos: Int) -> Unit = {}
            private set
        var onSwipeRight: (pos: Int) -> Unit = {}
            private set
        var pivotY: Pivot = Pivot.BOTTOM
            private set

        fun setShowItemsCount(itemsCount: Int) {
            shownItemsCount = itemsCount
        }

        fun setItemsSizeRatio(ratio: Float) {
            if (ratio !in 0f..1f) throw IllegalArgumentException("Ratio must be in the range 0..1")
            itemsSizeRatio = ratio
        }

        fun setElevationStep(step: Float) {
            elevationStep = step
        }

        fun setRelativeStackHeight(relativeHeight: Float) {
            if (relativeHeight !in 0f..1f)
                throw IllegalArgumentException("Relative height must be in the range 0..1")
            relativeStackHeight = relativeHeight
        }

        fun setRelativeSwipeThreshold(relativeThreshold: Float) {
            if (relativeThreshold !in 0f..1f)
                throw IllegalArgumentException("Relative threshold must be in the range 0..1")
            relativeSwipeThreshold = relativeThreshold
        }

        fun saveTopOnItemChanges(save: Boolean) {
            saveTopOnItemChanges = save
        }

        fun doOnSwipeLeft(action: (pos: Int) -> Unit) {
            onSwipeLeft = action
        }

        fun doOnSwipeRight(action: (pos: Int) -> Unit) {
            onSwipeRight = action
        }

        fun setPivotYType(pivotType: Pivot) {
            pivotY = pivotType
        }
    }

    companion object {
        private const val MAX_ROTATION_ANGLE = 12f
        private const val RECOVERY_DURATION = 200L
        private const val SWIPE_ON_EDGE_DURATION = 300L
        private const val APPEARANCE_DURATION = 800L

        private const val DEFAULT_SHOWN_ITEMS_COUNT = 5
        private const val DEFAULT_ELEVATION_STEP = 10f
        private const val DEFAULT_ITEMS_SIZE_RATIO = 0.93f
        private const val DEFAULT_RELATIVE_STACK_HEIGHT = 4 / 12f
        private const val DEFAULT_RELATIVE_SWIPE_THRESHOLD = 1 / 6f

        fun build(init: Config.() -> Unit): SwipeLayoutManager {
            val config = Config()
            config.init()
            return SwipeLayoutManager(config)
        }
    }
}
