package com.udacity.loading

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.udacity.loading.ButtonState.*
import com.udacity.R
import com.udacity.util.ext.disableViewDuringAnimation
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.properties.Delegates.observable

class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(
    context,
    attrs,
    defStyleAttr
) {
    // region Styling attributes
    private var loadingDefaultBackgroundColor = 0
    private var loadingBackgroundColor = 0
    private var loadingDefaultText: CharSequence = ""
    private var loadingText: CharSequence = ""
    private var loadingTextColor = 0
    private var progressCircleBackgroundColor = 0
    // endregion

    private var widthSize = 0
    private var heightSize = 0

    // region General Button variables
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    // endregion

    // region Button Text variables
    // It'll be initialized when styled attributes are retrieved
    // And it'll change whenever [buttonState] changes
    private var buttonText = ""
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55f
        typeface = Typeface.DEFAULT
    }

    // It'll be initialized when first Loading state is trigger
    private lateinit var buttonTextBounds: Rect
    // endregion

    // region Progress Circle/Arc variables
    private val progressCircleRect = RectF()
    private var progressCircleSize = 0f
    // endregion

    // region Animation variables
    private val animatorSet: AnimatorSet = AnimatorSet().apply {
        duration = THREE_SECONDS
        disableViewDuringAnimation(this@LoadingButton)
    }
    private var currentProgressCircleAnimationValue = 0f
    private val progressCircleAnimator = ValueAnimator.ofFloat(0f, FULL_ANGLE).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            currentProgressCircleAnimationValue = it.animatedValue as Float
            invalidate()
        }
    }
    private var currentButtonBackgroundAnimationValue = 0f
    private lateinit var buttonBackgroundAnimator: ValueAnimator
    // endregion

    // region LoadingButton state change handling
    private var buttonState: ButtonState by observable<ButtonState>(Completed) { _, _, newState ->
        Timber.d("Button state changed: $newState")
        when (newState) {
            Loading -> {
                // LoadingButton is now Loading and we need to set the correct text
                buttonText = loadingText.toString()

                // We only calculate ButtonText bounds and ProgressCircle rect once,
                // Only when buttonText is first initialized with loadingText
                if (!::buttonTextBounds.isInitialized) {
                    retrieveButtonTextBounds()
                    computeProgressCircleRect()
                }

                // ProgressCircle and Button background animations must start now
                animatorSet.start()
            }
            else -> {
                // LoadingButton is not doing any Loading so we need to reset to default text
                buttonText = loadingDefaultText.toString()

                // ProgressCircle animation must stop now
                newState.takeIf { it == Completed }?.run { animatorSet.cancel() }
            }
        }
    }

    /**
     * Initialize and retrieve the text boundary box of [buttonText] and store it into [buttonTextBounds].
     */
    private fun retrieveButtonTextBounds() {
        buttonTextBounds = Rect()
        buttonTextPaint.getTextBounds(buttonText, 0, buttonText.length, buttonTextBounds)
    }

    /**
     * Calculate left, top, right and bottom for [progressCircleRect] based on [buttonTextBounds]
     * and [heightSize].
     *
     * **This needs to be called after [retrieveButtonTextBounds()] call**
     */
    private fun computeProgressCircleRect() {
        val horizontalCenter =
            (buttonTextBounds.right + buttonTextBounds.width() + PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET)
        val verticalCenter = (heightSize / BY_HALF)

        progressCircleRect.set(
            horizontalCenter - progressCircleSize,
            verticalCenter - progressCircleSize,
            horizontalCenter + progressCircleSize,
            verticalCenter + progressCircleSize
        )
    }
    // endregion

    // region LoadingButton initialization
    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            loadingDefaultBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingDefaultBackgroundColor, 0)
            loadingBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            loadingDefaultText =
                getText(R.styleable.LoadingButton_loadingDefaultText)
            loadingTextColor =
                getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            loadingText =
                getText(R.styleable.LoadingButton_loadingText)
        }.also {
            buttonText = loadingDefaultText.toString()
            progressCircleBackgroundColor = ContextCompat.getColor(context, R.color.colorAccent)
        }
    }
    // endregion

    // region Important LoadingButton View methods to handle
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(
            minWidth,
            widthMeasureSpec,
            1
        )
        val h = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        progressCircleSize = (min(w, h) / BY_HALF) * PROGRESS_CIRCLE_SIZE_MULTIPLIER
        createButtonBackgroundAnimator()
    }

    /**
     * Creates and initializes [buttonBackgroundAnimator] that animates between 0f and the
     * [widthSize], which is recalculated in [onMeasure].
     */
    private fun createButtonBackgroundAnimator() {
        ValueAnimator.ofFloat(0f, widthSize.toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                currentButtonBackgroundAnimationValue = it.animatedValue as Float
                invalidate()
            }
        }.also {
            buttonBackgroundAnimator = it
            animatorSet.playProgressCircleAndButtonBackgroundTogether()
        }
    }

    /**
     * Sets up [animatorSet] to play [progressCircleAnimator] and [buttonBackgroundAnimator]
     * animations at the same time.
     *
     * **This needs to be called after [createButtonBackgroundAnimator] call**
     */
    private fun AnimatorSet.playProgressCircleAndButtonBackgroundTogether() =
        apply { playTogether(progressCircleAnimator, buttonBackgroundAnimator) }

    override fun performClick(): Boolean {
        super.performClick()
        // We only change button state to Clicked if the current state is Completed
        if (buttonState == Completed) {
            buttonState = Clicked
            invalidate()
        }
        return true
    }
    // endregion

    // region LoadingButton drawing
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { buttonCanvas ->
            Timber.d("LoadingButton onDraw()")
            buttonCanvas.apply {
                drawBackgroundColor()
                drawButtonText()
                drawProgressCircleIfLoading()
            }
        }
    }

    /**
     * Draws the button text using current value of [buttonText] which may change based on
     * the [buttonState].
     */
    private fun Canvas.drawButtonText() {
        // Draw the Loading Text at the Center of the Canvas
        // ref.: https://blog.danlew.net/2013/10/03/centering_single_line_text_in_a_canvas/
        buttonTextPaint.color = loadingTextColor
        drawText(
            buttonText,
            (widthSize / BY_HALF),
            (heightSize / BY_HALF) + buttonTextPaint.computeTextOffset(),
            buttonTextPaint
        )
    }

    /**
     * Calculates the height of the TextPaint using [TextPaint.ascent] and [TextPaint.descent].
     * These measure the size above/below the text's baseline. Combined, they add up to the total
     * height of the drawn text.
     */
    private fun TextPaint.computeTextOffset() = ((descent() - ascent()) / 2) - descent()

    /**
     * Draws the default button background color using [loadingBackgroundColor].
     */
    private fun Canvas.drawBackgroundColor() {
        when (buttonState) {
            Loading -> {
                drawLoadingBackgroundColor()
                drawDefaultBackgroundColor()
            }
            else -> drawColor(loadingDefaultBackgroundColor)
        }
    }

    /**
     * Draws the [Rect] with [loadingBackgroundColor] representing the loading progress.
     */
    private fun Canvas.drawLoadingBackgroundColor() = buttonPaint.apply {
        color = loadingBackgroundColor
    }.run {
        drawRect(
            0f,
            0f,
            currentButtonBackgroundAnimationValue,
            heightSize.toFloat(),
            buttonPaint
        )
    }

    /**
     * Draws the [Rect] with [loadingDefaultBackgroundColor] representing the background of the button.
     */
    private fun Canvas.drawDefaultBackgroundColor() = buttonPaint.apply {
        color = loadingDefaultBackgroundColor
    }.run {
        drawRect(
            currentButtonBackgroundAnimationValue,
            0f,
            widthSize.toFloat(),
            heightSize.toFloat(),
            buttonPaint
        )
    }

    /**
     * Draws progress circle if [buttonState] is [ButtonState.Loading].
     */
    private fun Canvas.drawProgressCircleIfLoading() =
        buttonState.takeIf { it == Loading }?.let { drawProgressCircle(this) }

    /**
     * Draws the progress circle using an arc only when [buttonState] changes to [ButtonState.Loading].
     * The sweep angle uses [currentProgressCircleAnimationValue] which is changed according to when
     * [progressCircleAnimator] send updates after the values for the animation have been calculated.
     */
    private fun drawProgressCircle(buttonCanvas: Canvas) {
        buttonPaint.color = progressCircleBackgroundColor
        buttonCanvas.drawArc(
            progressCircleRect,
            0f,
            currentProgressCircleAnimationValue,
            true,
            buttonPaint
        )
    }
    // endregion

    // region Public LoadingButton API
    /**
     * Changes [buttonState] to the given [state] if they are not the same causing the view to be
     * redrawn.
     */
    fun changeButtonState(state: ButtonState) {
        if (state != buttonState) {
            buttonState = state
            invalidate()
        }
    }
    // endregion

    // region LoadingButton constants
    companion object {
        private const val PROGRESS_CIRCLE_SIZE_MULTIPLIER = 0.4f
        private const val PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET = 16f
        private const val BY_HALF = 2f
        private const val FULL_ANGLE = 360f
        private val THREE_SECONDS = TimeUnit.SECONDS.toMillis(3)
    }
    // endregion
}