package com.udacity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.udacity.ButtonState.*
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
    private var loadingBackgroundColor = 0
    private var loadingTextColor = 0
    private var loadingDefaultText: CharSequence = ""
    private var loadingText: CharSequence = ""

    private var widthSize = 0
    private var heightSize = 0

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

    private var currentProgressCircleAnimationValue = 0f
    private val progressCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.colorAccent)
    }
    private val progressCircleRect = RectF()
    private val progressCircleAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        duration = TimeUnit.SECONDS.toMillis(3)
        addUpdateListener {
            currentProgressCircleAnimationValue = it.animatedValue as Float
            invalidate()
        }
        disableViewDuringAnimation(this@LoadingButton)
    }
    private var progressCircleSize = 0f

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

                // ProgressCircle animation must start now
                progressCircleAnimator.start()
            }
            else -> {
                // LoadingButton is not doing any Loading so we need to reset to default text
                buttonText = loadingDefaultText.toString()

                // ProgressCircle animation must stop now
                newState.takeIf { it == Completed }?.run { progressCircleAnimator.cancel() }
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

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            loadingBackgroundColor = getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            loadingTextColor = getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            loadingDefaultText = getText(R.styleable.LoadingButton_loadingDefaultText)
            loadingText = getText(R.styleable.LoadingButton_loadingText)
        }.also {
            buttonText = loadingDefaultText.toString()
        }
    }

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
    }

    override fun performClick(): Boolean {
        super.performClick()
        // We only change button state to Clicked if the current state is Completed
        if (buttonState == Completed) {
            buttonState = Clicked
            invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { buttonCanvas ->
            Timber.d("LoadingButton onDraw()")

            drawBackgroundColor(buttonCanvas)
            drawButtonText(buttonCanvas)

            if (buttonState == Loading) drawProgressCircle(buttonCanvas)
        }
    }

    /**
     * Draw the default button background color using [loadingBackgroundColor]
     */
    private fun drawBackgroundColor(buttonCanvas: Canvas) {
        buttonCanvas.drawColor(loadingBackgroundColor)
    }

    /**
     * Draw the button text using current value of [buttonText] which may change based on
     * the [buttonState].
     */
    private fun drawButtonText(buttonCanvas: Canvas) {
        // Draw the Loading Text at the Center of the Canvas
        // ref.: https://blog.danlew.net/2013/10/03/centering_single_line_text_in_a_canvas/
        buttonTextPaint.color = loadingTextColor
        val textHeight = buttonTextPaint.descent() - buttonTextPaint.ascent()
        val textOffset = (textHeight / 2) - buttonTextPaint.descent()

        buttonCanvas.drawText(
            buttonText,
            (widthSize / BY_HALF),
            (heightSize / BY_HALF) + textOffset,
            buttonTextPaint
        )
    }

    /**
     * Draw the progress circle using an arc only when [buttonState] changes to [ButtonState.Loading].
     * The sweep angle uses [currentProgressCircleAnimationValue] which is changed according to when
     * [progressCircleAnimator] send updates after the values for the animation have been calculated.
     */
    private fun drawProgressCircle(buttonCanvas: Canvas) {
        buttonCanvas.drawArc(
            progressCircleRect,
            0f,
            currentProgressCircleAnimationValue,
            true,
            progressCirclePaint
        )
    }

    /**
     * Change [buttonState] to the given [state] if they are not the same causing the view to be
     * redrawn.
     */
    fun changeButtonState(state: ButtonState) {
        if (state != buttonState) {
            buttonState = state
            invalidate()
        }
    }

    companion object {
        private const val PROGRESS_CIRCLE_SIZE_MULTIPLIER = 0.4f
        private const val PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET = 16f
        private const val BY_HALF = 2f
    }
}