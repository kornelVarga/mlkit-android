package com.kornelvarga.mlkitdemo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.kornelvarga.mlkitdemo.R
import com.kornelvarga.mlkitdemo.model.Prediction

class ImageRecognitionChart(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        const val COLUMN_WIDTH = 100f
        const val TOP_PADDING = 50f
        const val LEFT_PADDING = 140f
        const val RIGHT_PADDING = 100f
    }

    private val labelPaint = Paint()
    private val columnPaint = Paint()
    private val verticalLinePaint = Paint()
    private val predictionPaint = Paint()

    private var predictions = ArrayList<Prediction>()

    init {
        labelPaint.color = context.getColor(R.color.orangeLight)
        labelPaint.textAlign = Paint.Align.RIGHT
        labelPaint.textSize = 25f

        columnPaint.color = context.getColor(R.color.white)
        columnPaint.style = Paint.Style.FILL

        verticalLinePaint.color = context.getColor(R.color.orangeLight)
        verticalLinePaint.strokeWidth = 10f

        predictionPaint.color = context.getColor(R.color.orangeLight)
        predictionPaint.textAlign = Paint.Align.LEFT
        predictionPaint.textSize = 25f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawColumns(canvas)
        drawVerticalLine(canvas)
        drawLabels(canvas)
    }

    private fun drawLabels(canvas: Canvas) {
        val labelPositionX = LEFT_PADDING - 10f
        for ((i, prediction) in predictions.withIndex()) {
            canvas.drawText(prediction.label, labelPositionX, TOP_PADDING + ((COLUMN_WIDTH / 2 + COLUMN_WIDTH * i) - ((labelPaint.descent() + labelPaint.ascent()) / 2)), labelPaint)
            //canvas.drawText("%.2f".format(prediction.probability * 100) + "%", width.toFloat()- RIGHT_PADDING, TOP_PADDING + ((COLUMN_WIDTH / 2 + COLUMN_WIDTH * i) - ((predictionPaint.descent() + predictionPaint.ascent()) / 2)), predictionPaint)
        }
    }

    private fun drawColumns(canvas: Canvas) {
        val predictionPositionX = LEFT_PADDING + 10
        val w = width - RIGHT_PADDING
        for ((i, prediction) in predictions.withIndex()) {
            canvas.drawRoundRect(
                LEFT_PADDING,
                TOP_PADDING + (COLUMN_WIDTH * i + 10),
                ((w - LEFT_PADDING) * prediction.probability) + LEFT_PADDING,
                TOP_PADDING + (COLUMN_WIDTH * (i + 1) - 10),
                10f,
                10f,
                columnPaint
            )
            canvas.drawText("%.2f".format(prediction.probability * 100) + "%",
                ((w - LEFT_PADDING) * prediction.probability) + predictionPositionX,
                TOP_PADDING + ((COLUMN_WIDTH / 2 + COLUMN_WIDTH * i) - ((predictionPaint.descent() + predictionPaint.ascent()) / 2)),
                predictionPaint)

        }
    }

    private fun drawVerticalLine(canvas: Canvas) {
        canvas.drawLine(LEFT_PADDING, TOP_PADDING, LEFT_PADDING, (COLUMN_WIDTH) * 3 + TOP_PADDING, verticalLinePaint)
    }

    fun setPredictions(predictions: ArrayList<Prediction>) {
        this.predictions = predictions
        invalidate()
    }

}