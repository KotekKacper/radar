/*
 * Copyright 2022 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid.ble

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import com.punchthrough.blestarterappandroid.R
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * ========================================================================
 * @author: Created by Vension on 2019/9/17 16:10.
 * @email : vensionHu@qq.com
 * @Github: https://github.com/Vension
 * __      ________ _   _  _____ _____ ____  _   _
 * \ \    / /  ____| \ | |/ ____|_   _/ __ \| \ | |
 *  \ \  / /| |__  |  \| | (___   | || |  | |  \| |
 *   \ \/ / |  __| | . ` |\___ \  | || |  | | . ` |
 *    \  /  | |____| |\  |____) |_| || |__| | |\  |
 *     \/   |______|_| \_|_____/|_____\____/|_| \_|
 *
 * Take advantage of youth and toss about !
 * ------------------------------------------------------------------------
 * @desc: happy code -> 自定义雷达扫描效果控件
 * ========================================================================
 */
class RadarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val DEFAULT_COLOR : Int = Color.parseColor("#FF0000")
    private var mCircleColor = DEFAULT_COLOR
    private var mCircleNum = 3
    private var mSweepColor = DEFAULT_COLOR
    private var mRaindropColor = DEFAULT_COLOR
    private var mRaindropNum = 5
    private var isShowCrossLine = true
    private var isShowRaindrop = true
    private var mSpeed = 3.0f
    private var mFlicker = 3.0f
    private var mRaindropStartSize = 15.0f
    private var mRaindropVanishSize = 35.0f

    private lateinit var mCirclePaint: Paint
    private lateinit var mSweepPaint: Paint
    private lateinit var mRaindropPaint: Paint

    private var mDegrees: Float = 0.toFloat()
    private var isScanning = false

    private val mRaindrops = ArrayList<Raindrop>()
    private val mAngles = ArrayList<Int>()
    private var ang = 0

    init {
        initAttrs(context,attrs)
        initPaints()
    }

    private fun initAttrs(context: Context?, attrs: AttributeSet?) {
        attrs?.let {
            val mTypedArray = context!!.obtainStyledAttributes(attrs, R.styleable.RadarView)
            mCircleColor = mTypedArray.getColor(R.styleable.RadarView_kv_circleColor, DEFAULT_COLOR)
            mCircleNum = mTypedArray.getInt(R.styleable.RadarView_kv_circleNum, mCircleNum)
            if (mCircleNum < 1) {
                mCircleNum = 3
            }
            mSweepColor = mTypedArray.getColor(R.styleable.RadarView_kv_sweepColor, DEFAULT_COLOR)
            mRaindropColor =
                mTypedArray.getColor(R.styleable.RadarView_kv_raindropColor, DEFAULT_COLOR)
            mRaindropNum = mTypedArray.getInt(R.styleable.RadarView_kv_raindropNum, mRaindropNum)
            isShowCrossLine = mTypedArray.getBoolean(R.styleable.RadarView_kv_showCrossLine, true)
            isShowRaindrop = mTypedArray.getBoolean(R.styleable.RadarView_kv_showRaindrop, true)
            mSpeed = mTypedArray.getFloat(R.styleable.RadarView_kv_speed, mSpeed)
            if (mSpeed <= 0) {
                mSpeed = 3f
            }
            mFlicker = mTypedArray.getFloat(R.styleable.RadarView_kv_flicker, mFlicker)
            if (mFlicker <= 0) {
                mFlicker = 3f
            }
            mTypedArray.recycle()
        }
    }

    private fun initPaints() {
        mCirclePaint = Paint().apply {
            color = mCircleColor
            strokeWidth = 1.0f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        mRaindropPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        mSweepPaint = Paint().apply {
            isAntiAlias = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defauleSize = dp2px(context,200f)
        setMeasuredDimension(measureWidth(widthMeasureSpec,defauleSize),measureHeight(heightMeasureSpec,defauleSize))
    }

    private fun measureWidth(measureSpec: Int, defaultSize: Int): Int {
        var result:Int
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = defaultSize + paddingLeft + paddingRight
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        result = max(result, suggestedMinimumWidth)
        return result
    }

    private fun measureHeight(measureSpec: Int, defaultSize: Int): Int {
        var result : Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = defaultSize + paddingTop + paddingBottom
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        result = max(result, suggestedMinimumHeight)
        return result
    }


    override fun onDraw(canvas: Canvas?) {
        val mWidth = width - paddingLeft - paddingRight
        val mHeight = height - paddingTop - paddingBottom
        val radius = min(mWidth, mHeight) / 2

        val cx = paddingLeft + (width - paddingLeft - paddingRight) / 2
        val cy = paddingTop + (height - paddingTop - paddingBottom) / 2

        drawCircle(canvas!!, cx, cy, radius) // circles inside

        if (isShowCrossLine) {
            drawCrossLine(canvas, cx, cy, radius)
        }

        if (isScanning) {
            if (isShowRaindrop) {
                drawRaindrop(canvas, cx, cy, radius)
            }
            drawSweep(canvas, cx, cy, radius)
            mDegrees = (mDegrees + 360f / mSpeed / 60f) % 360

            invalidate()
        }
    }

    private fun drawCircle(canvas: Canvas, cx: Int, cy: Int, radius: Int) {
        for (i in 0 until mCircleNum) {
            canvas.drawCircle(
                cx.toFloat(),
                cy.toFloat(),
                (radius - radius / mCircleNum * i).toFloat(),
                mCirclePaint
            )
        }
    }

    private fun drawCrossLine(canvas: Canvas, cx: Int, cy: Int, radius: Int) {
        canvas.drawLine(
            (cx - radius).toFloat(),
            cy.toFloat(),
            (cx + radius).toFloat(),
            cy.toFloat(),
            mCirclePaint)
        canvas.drawLine(
            cx.toFloat(),
            (cy - radius).toFloat(),
            cx.toFloat(),
            (cy + radius).toFloat(),
            mCirclePaint
        )
    }

    fun drawRaindrop(canvas: Canvas, cx: Int, cy: Int, radius: Int) {
        for (raindrop in mRaindrops) {
            mRaindropPaint.color = raindrop.changeAlpha()
            canvas.drawCircle(
                raindrop.x.toFloat(),
                raindrop.y.toFloat(),
                raindrop.radius,
                mRaindropPaint
            )
            raindrop.radius += 1.0f * 20 / 60f / mFlicker
            raindrop.alpha -= 1.0f * 255 / 60f / mFlicker
        }
        removeRaindrop()
    }


    private fun drawSweep(canvas: Canvas, cx: Int, cy: Int, radius: Int) {
        val sweepGradient = SweepGradient(
            cx.toFloat(), cy.toFloat(),
            intArrayOf(
                Color.TRANSPARENT,
                changeAlpha(mSweepColor, 0),
                changeAlpha(mSweepColor, 168),
                changeAlpha(mSweepColor, 255),
                changeAlpha(mSweepColor, 255)
            ), floatArrayOf(0.0f, 0.6f, 0.99f, 0.998f, 1f)
        )
        mSweepPaint.shader = sweepGradient

        if (mAngles.isNotEmpty()){
            ang = -mAngles[0]
            mAngles.removeAt(0)
        }
        canvas.rotate(ang.toFloat(), cx.toFloat(), cy.toFloat())
        canvas.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), mSweepPaint)
    }

    @SuppressLint("LogNotTimber")
    fun addRaindrop(x: Int, y: Int){
        Log.i("x,y", "$x, $y")
        mRaindrops.add(Raindrop(x, y, mRaindropStartSize, mRaindropColor))
        invalidate()
    }
    fun addAngle(angle: Int){
        mAngles.add(angle)
    }

    private fun removeRaindrop() {
        val iterator = mRaindrops.iterator()
        while (iterator.hasNext()) {
            val raindrop = iterator.next()
            if (raindrop.radius > mRaindropVanishSize || raindrop.alpha < 0) {
                iterator.remove()
            }
        }
    }

    fun start() {
        if (!isScanning) {
            isScanning = true
            invalidate()
        }
    }

    fun stop() {
        if (isScanning) {
            isScanning = false
            mRaindrops.clear()
            mDegrees = 0.0f
        }
    }

    private fun changeAlpha(color: Int, alpha: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    private fun dp2px(context: Context, dpVal: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal, context.resources.displayMetrics).toInt()
    }

    private inner class Raindrop(
        internal var x: Int,
        internal var y: Int,
        internal var radius: Float,
        internal var color: Int) {

        internal var alpha = 255f

        internal fun changeAlpha(): Int {
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            return Color.argb(alpha.toInt(), red, green, blue)
        }

    }

}