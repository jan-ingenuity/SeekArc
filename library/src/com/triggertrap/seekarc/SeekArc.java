/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Triggertrap Ltd
 * Author Neil Davies
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.triggertrap.seekarc;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 
 * SeekArc.java
 * 
 * This is a class that functions much like a SeekBar but
 * follows a circle path instead of a straight line.
 * 
 * @author Neil Davies
 * 
 */
public class SeekArc extends View {
	private static final String TAG = SeekArc.class.getSimpleName();

	private static int INVALID_PROGRESS_VALUE = -1;
	private static double INVALID_ANGLE_VALUE = 999.00;

	// The initial rotational offset -90 means we start at 12 o'clock
	private final int mAngleOffset = -90;

	/**
	 * The Drawable for the seek arc thumbnail
	 */
	private Drawable mThumb;
	
	/**
	 * The Maximum value that this SeekArc can be set to
	 */
	private int mMax = 100;
	
	/**
	 * The Current value that the SeekArc is set to
	 */
	private int mProgress = 0;
		
	/**
	 * The width of the progress line for this SeekArc
	 */
	private int mProgressWidth = 4;
	
	/**
	 * The Width of the background arc for the SeekArc 
	 */
	private int mArcWidth = 2;

	/**
	 * The dimension of allowance that the arc will accept inputs outside of the arc drawing
	 */
	private int mBoundsInner = 0;
	private int mBoundsOuter = 0;
	
	/**
	 * The Angle to start drawing this Arc from
	 */
	private int mStartAngle = 0;
	
	/**
	 * The Angle through which to draw the arc (Max is 360)
	 */
	private int mSweepAngle = 360;
	private int mSweepNormal = mSweepAngle; // use value here when reverting to non-continuous mode
	
	/**
	 * The rotation of the SeekArc- 0 is twelve o'clock
	 */
	private int mRotation = 0;
	
	/**
	 * Give the SeekArc rounded edges
	 */
	private boolean mRoundedEdges = false;
	
	/**
	 * Will the progress increase clockwise or anti-clockwise
	 */
	private boolean mClockwise = true;

	/**
	 * is the control enabled/touchable
 	 */
	private boolean mEnabled = true;

	// Internal variables

	private boolean mContinuous = false;
	private boolean mAccepts = false;
	private boolean mDragging = false;

	private int mThumbOffset = 0;
	private int mThumbRadius = 0;
//	private int mThumbInnerRadius;
//	private int mThumbOuterRadius;
	private int mArcRadius = 0;
	private int mBoundsInnerRadius;
	private int mBoundsOuterRadius;
	private int mTranslateX;
	private int mTranslateY;
	private int mThumbXPos;
	private int mThumbYPos;
	private double mTouchAngleStart;
	private double mTouchAnglePrev;
	private double mTouchAngle = INVALID_ANGLE_VALUE;
	private float mProgressSweep = 0;
	private float mProposedSweep = 0;

	private RectF mArcRect = new RectF();
	private Paint mArcPaint;
	private Paint mProgressPaint;
	private Paint mProposedPaint;
	private OnSeekArcChangeListener mOnSeekArcChangeListener;
	private Path.Direction mDirection;

	public interface OnSeekArcChangeListener {

		/**
		 * Notification that the progress level has changed. Clients can use the
		 * fromUser parameter to distinguish user-initiated changes from those
		 * that occurred programmatically.
		 * 
		 * @param seekArc
		 *            The SeekArc whose progress has changed
		 * @param progress
		 *            The current progress level. This will be in the range
		 *            0..max where max was set by
		 *            {@link SeekArc#setMax(int)}. (The default value for
		 *            max is 100.)
		 * @param fromUser
		 *            True if the progress change was initiated by the user.
		 */
		void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser);

		/**
		 * Notification that the user has started a touch gesture. Clients may
		 * want to use this to disable advancing the seekbar.
		 * 
		 * @param seekArc
		 *            The SeekArc in which the touch gesture began
		 */
		void onStartTrackingTouch(SeekArc seekArc);

		/**
		 * Notification that the user has finished a touch gesture. Clients may
		 * want to use this to re-enable advancing the seekarc.
		 * 
		 * @param seekArc
		 *            The SeekArc in which the touch gesture began
		 */
		void onStopTrackingTouch(SeekArc seekArc);
	}

	public SeekArc(Context context) {
		super(context);
		init(context, null, 0);
	}

	public SeekArc(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, R.attr.seekArcStyle);
	}

	public SeekArc(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	private void init(Context context, AttributeSet attrs, int defStyle) {
		Log.d(TAG, "Initialising SeekArc");

		final Resources res = getResources();
		float density = context.getResources().getDisplayMetrics().density;

		// Defaults, may need to link this into theme settings
		int arcColor = res.getColor(R.color.progress_gray);
		int progressColor = res.getColor(R.color.default_blue_light);
		int proposedColor = res.getColor(R.color.proposed_red);

		int thumbHalfHeight;
		int thumbHalfWidth;

		boolean continuous = false;

		mThumb = res.getDrawable(R.drawable.seek_arc_control_selector);
		// Convert progress width to pixels for current density
		mProgressWidth = (int) (mProgressWidth * density);
		mBoundsInner = 0;
		mBoundsOuter = 0;

		
		if (attrs != null) {
			// Attribute initialization
			final TypedArray a = context.obtainStyledAttributes(
				attrs,
				R.styleable.SeekArc,
				defStyle,
				0
			);

			Drawable thumb = a.getDrawable(R.styleable.SeekArc_thumb);
			if (thumb != null) {
				mThumb = thumb;
			}

			thumbHalfHeight = mThumb.getIntrinsicHeight() / 2;
			thumbHalfWidth = mThumb.getIntrinsicWidth() / 2;
			mThumb.setBounds(-thumbHalfWidth, -thumbHalfHeight, thumbHalfWidth, thumbHalfHeight);

			mThumbOffset = (int) a.getDimension(R.styleable.SeekArc_thumbOffset, mThumbOffset);

			mMax = a.getInteger(R.styleable.SeekArc_max, mMax);
			mProgress = a.getInteger(R.styleable.SeekArc_progress, mProgress);

			mRotation = a.getInt(R.styleable.SeekArc_rotation, mRotation);
			mStartAngle = a.getInt(R.styleable.SeekArc_startAngle, mStartAngle);
			mSweepAngle = a.getInt(R.styleable.SeekArc_sweepAngle, mSweepAngle);

			mProgressWidth = (int) a.getDimension(R.styleable.SeekArc_progressWidth, mProgressWidth);
			mArcWidth = (int) a.getDimension(R.styleable.SeekArc_arcWidth, mArcWidth);
			mBoundsInner = (int) a.getDimension(R.styleable.SeekArc_boundsInner, mBoundsInner);
			mBoundsOuter = (int) a.getDimension(R.styleable.SeekArc_boundsOuter, mBoundsOuter);

			mRoundedEdges = a.getBoolean(R.styleable.SeekArc_roundEdges, mRoundedEdges);
			mClockwise = a.getBoolean(R.styleable.SeekArc_clockwise, mClockwise);
			mEnabled = a.getBoolean(R.styleable.SeekArc_enabled, mEnabled);

			continuous = a.getBoolean(R.styleable.SeekArc_continuous, continuous);

			arcColor = a.getColor(R.styleable.SeekArc_arcColor, arcColor);
			progressColor = a.getColor(R.styleable.SeekArc_progressColor, progressColor);
			proposedColor = a.getColor(R.styleable.SeekArc_proposedColor, proposedColor);

			a.recycle();
		}

		mProgress = (mProgress > mMax) ? mMax : mProgress;
		mProgress = (mProgress < 0) ? 0 : mProgress;

		mSweepAngle = (mSweepAngle > 360) ? 360 : mSweepAngle;
		mSweepAngle = (mSweepAngle < 0) ? 0 : mSweepAngle;
		mSweepNormal = mSweepAngle;

		mProgressSweep = (float) mProgress / mMax * mSweepAngle;

		mStartAngle = (mStartAngle > 360) ? 0 : mStartAngle;
		mStartAngle = (mStartAngle < 0) ? 0 : mStartAngle;

		// Dotted lines effect ish
		// PathEffect effect = new DashPathEffect(new float[]{50, 50}, 0);

		mArcPaint = new Paint();
		mArcPaint.setColor(arcColor);
		mArcPaint.setAntiAlias(true);
		mArcPaint.setStyle(Paint.Style.STROKE);
		mArcPaint.setStrokeWidth(mArcWidth);
		//mArcPaint.setPathEffect(effect);
		//mArcPaint.setAlpha(45);

		mProgressPaint = new Paint();
		mProgressPaint.setColor(progressColor);
		mProgressPaint.setAntiAlias(true);
		mProgressPaint.setStyle(Paint.Style.STROKE);
		mProgressPaint.setStrokeWidth(mProgressWidth);
		//mProgressPaint.setPathEffect(effect);

		mProposedPaint = new Paint();
		mProposedPaint.setColor(proposedColor);
		mProposedPaint.setAntiAlias(true);
		mProposedPaint.setStyle(Paint.Style.STROKE);
		mProposedPaint.setStrokeWidth(mProgressWidth);
		//mProposedPaint.setPathEffect(effect);

		setContinuous(continuous);

		if (mRoundedEdges) {
			mArcPaint.setStrokeCap(Paint.Cap.ROUND);
			mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
			mProposedPaint.setStrokeCap(Paint.Cap.ROUND);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if(!mClockwise) {
			canvas.scale(-1, 1, mArcRect.centerX(), mArcRect.centerY() );
		}
		
		// Draw the arcs
		final int arcStart = mStartAngle + mAngleOffset + mRotation;
		final int arcSweep = mSweepAngle;

		canvas.drawArc(mArcRect, arcStart, arcSweep, false, mArcPaint);

		if (!mContinuous && mProgressSweep > 0)
			canvas.drawArc(mArcRect, arcStart, mProgressSweep, false, mProgressPaint);

		float proposedStart;
		float proposedSweep;

		if (mContinuous) {
			proposedStart = (float)(mTouchAngleStart + mAngleOffset);
			proposedSweep = mProposedSweep;
		}
		else {
			// start: at the current progress degree
			// sweep: only the different between the proposed and current progress.
			proposedStart = arcStart + mProgressSweep;
			proposedSweep = mProposedSweep - mProgressSweep;
		}

		if (mDragging && proposedSweep != 0)
			canvas.drawArc(mArcRect, proposedStart, proposedSweep, false, mProposedPaint);

		if (mEnabled) {
			// Draw the thumb nail
			canvas.translate(mTranslateX - mThumbXPos, mTranslateY - mThumbYPos);
			mThumb.draw(canvas);
		}
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		final int min = Math.min(width, height);
		float top;
		float left;
		float arcRadius;
		float arcSweep;
		int arcWidth;
		int arcStart;
		int arcDiameter;

		arcWidth = Math.max(mArcWidth, mProgressWidth);
		arcDiameter = min - getPaddingLeft() - arcWidth;
		arcRadius = arcDiameter / 2;
		top = height / 2 - arcRadius;
		left = width / 2 - arcRadius;

		mTranslateX = (int) (width * 0.5f);
		mTranslateY = (int) (height * 0.5f);
		mArcRadius = (int)arcRadius;
		mThumbRadius = mArcRadius - mThumbOffset;

		measureBounds();

		mArcRect.set(left, top, left + arcDiameter, top + arcDiameter);

		arcSweep = mDragging ? mProposedSweep : mProgressSweep;
		arcStart = (int)arcSweep + mStartAngle  + mRotation + -mAngleOffset;

		mThumbXPos = (int) (mThumbRadius * Math.cos(Math.toRadians(arcStart)));
		mThumbYPos = (int) (mThumbRadius * Math.sin(Math.toRadians(arcStart)));

		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mEnabled) {
			this.getParent().requestDisallowInterceptTouchEvent(true);

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					onStartTrackingTouch();
					updateOnTouch(event);
					break;
				case MotionEvent.ACTION_MOVE:
					updateOnTouch(event);
					break;
				case MotionEvent.ACTION_UP:
					onStopTrackingTouch();
					setPressed(false);
					this.getParent().requestDisallowInterceptTouchEvent(false);
					break;
				case MotionEvent.ACTION_CANCEL:
					onStopTrackingTouch();
					setPressed(false);
					this.getParent().requestDisallowInterceptTouchEvent(false);
					break;
			}
			return true;
		}
		return false;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mThumb != null && mThumb.isStateful()) {
			int[] state = getDrawableState();
			mThumb.setState(state);
		}
		invalidate();
	}

	private void measureBounds() {
		int boundsThickness;
		boundsThickness = Math.max(mArcWidth, mProgressWidth);
		boundsThickness /= 2;

		mBoundsOuterRadius = mArcRadius + mBoundsOuter + boundsThickness;
		mBoundsInnerRadius = mArcRadius - mBoundsInner - boundsThickness;

//		int thumbSize;
//		thumbSize = Math.min(mThumb.getIntrinsicWidth(), mThumb.getIntrinsicHeight());
//		thumbSize = thumbSize / 2;
//
//		mThumbInnerRadius = mThumbRadius - thumbSize;
//		mThumbOuterRadius = mThumbRadius + thumbSize;
	}

	private void onStartTrackingTouch() {
		if (mOnSeekArcChangeListener != null) {
			mOnSeekArcChangeListener.onStartTrackingTouch(this);
		}
	}

	private void onStopTrackingTouch() {
		if (mOnSeekArcChangeListener != null) {
			mOnSeekArcChangeListener.onStopTrackingTouch(this);
		}
	}

	private void updateOnTouch(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();

		int action = event.getAction();

		if (action == MotionEvent.ACTION_DOWN)
			mAccepts = !ignoreTouch(x, y);

		if (!mAccepts)
			return;

		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
			setPressed(true);
			mDragging = true;
		}
		else {
			setPressed(false);
			mDragging = false;
		}

		mTouchAnglePrev = mTouchAngle;
		mTouchAngle = getTouchDegrees(x, y);

		if (action == MotionEvent.ACTION_DOWN)
			mTouchAngleStart = mTouchAngle;

		mDirection = getTouchDirection();

		onProgressRefresh(getProgressForAngle(mTouchAngle), true);
	}

	private boolean ignoreTouch(float xPos, float yPos) {
		float x = xPos - mTranslateX;
		float y = yPos - mTranslateY;
		float touchRadius = (float) Math.sqrt(((x * x) + (y * y)));

		return touchRadius < mBoundsInnerRadius || touchRadius > mBoundsOuterRadius;

//		return (touchRadius < mBoundsInnerRadius || touchRadius > mBoundsOuterRadius)
//			&& (touchRadius < mThumbInnerRadius || touchRadius > mThumbOuterRadius);
	}

	private double getTouchDegrees(float xPos, float yPos) {
		float x = xPos - mTranslateX;
		float y = yPos - mTranslateY;

		//invert the x-coord if we are rotating anti-clockwise
		x = (mClockwise) ? x : -x;

		// convert to arc Angle
		double angle;

		angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2) - Math.toRadians(mRotation));
		angle -= mStartAngle;
		angle += angle < 0 ? 360 : 0;

		return angle;
	}

	private Path.Direction getTouchDirection() {
		double startLower = (mTouchAngleStart - 1) % 360,
			   startUpper = (mTouchAngleStart + 1) % 360;

		if (mDirection != null)
			if (startLower < mTouchAngle && mTouchAngle < startUpper)
				return null;
			else
				return mDirection;

//		if ((int)mTouchAngle == (int)mTouchAngleStart)
//			return null;
//
//		if (mDirection != null)
//			return mDirection;

		else if (mTouchAngle < mTouchAngleStart || mTouchAngle == 360 && mTouchAngleStart == 0)
			return Path.Direction.CCW;

		else if (mTouchAngle > mTouchAngleStart|| mTouchAngleStart == 0 && mTouchAngle == 360)
			return Path.Direction.CW;

		return mDirection;
	}

	private int getProgressForAngle(double angle) {
		int touchProgress = (int) Math.round(valuePerDegree() * angle);

		touchProgress = (touchProgress < 0) ? INVALID_PROGRESS_VALUE : touchProgress;
		touchProgress = (touchProgress > mMax) ? INVALID_PROGRESS_VALUE : touchProgress;

		return touchProgress;
	}

	private float valuePerDegree() {
		return (float) mMax / mSweepAngle;
	}

	private void onProgressRefresh(int progress, boolean fromUser) {
		if (mDragging)
			updateDelta(progress, fromUser);
		else
			updateProgress(mProgress, fromUser);
	}

	private void updateThumbPosition() {
		float sweep;
		int angle;

		if (mContinuous)
			sweep = mProgressSweep;
		else
			sweep = mDragging ? mProposedSweep : mProgressSweep;

		angle = (int) (mStartAngle + sweep + mRotation - mAngleOffset);

		mThumbXPos = (int)(mThumbRadius * Math.cos(Math.toRadians(angle)));
		mThumbYPos = (int)(mThumbRadius * Math.sin(Math.toRadians(angle)));
	}
	
	private void updateProgress(int progress, boolean fromUser) {
		if (progress == INVALID_PROGRESS_VALUE)
			return;

		if (mOnSeekArcChangeListener != null)
			mOnSeekArcChangeListener.onProgressChanged(this, progress, fromUser);

		mProgressSweep = (float) progress / mMax * mSweepAngle;

		updateThumbPosition();
		invalidate();
	}

	private void updateDelta(int progress, boolean fromUser) {
		if (progress == INVALID_PROGRESS_VALUE)
			return;

		progress = progress > mMax ? mMax : progress;
		progress = progress < 0 ? 0 : progress;

		mProgress = progress;

		if (mOnSeekArcChangeListener != null)
			mOnSeekArcChangeListener.onProgressChanged(this, progress, fromUser);

		if (mContinuous) {
			if (mDirection == Path.Direction.CW) {
				mProposedSweep = (float) (mTouchAngle - mTouchAngleStart);
				mProposedSweep = mProposedSweep >= 0
							   ? mProposedSweep
							   : (float) (360 - mTouchAngleStart + mTouchAngle);
			}
			else if (mDirection == Path.Direction.CCW) {
				mProposedSweep = (float) (mTouchAngle - mTouchAngleStart);
				mProposedSweep = mProposedSweep <= 0
							   ? mProposedSweep
							   : (float) (mTouchAngle - 360 - mTouchAngleStart);
			}
			else
				mProposedSweep = 0;

			mProgressSweep = (float)progress / mMax * mSweepAngle;
		}
		else
			mProposedSweep = (float)progress / mMax * mSweepAngle;

		updateThumbPosition();
		invalidate();
	}

	/**
	 * Sets a listener to receive notifications of changes to the SeekArc's
	 * progress level. Also provides notifications of when the user starts and
	 * stops a touch gesture within the SeekArc.
	 * 
	 * @param l
	 *            The seek bar notification listener
	 * 
	 * @see SeekArc.OnSeekArcChangeListener
	 */
	public void setOnSeekArcChangeListener(OnSeekArcChangeListener l) {
		mOnSeekArcChangeListener = l;
	}

	public void setProgress(int progress) {
		updateProgress(progress, false);
	}

	public void setDelta(int progress) {
		mDragging = true;
		updateDelta(progress, false);
	}

	public void commit() {
		updateProgress(mProgress, false);
	}

	public void rollback() {

	}

	public int getProgress() {
		return mProgress;
	}

	public int getProgressWidth() {
		return mProgressWidth;
	}

	public void setProgressWidth(int mProgressWidth) {
		this.mProgressWidth = mProgressWidth;
		mProgressPaint.setStrokeWidth(mProgressWidth);
	}
	
	public int getArcWidth() {
		return mArcWidth;
	}

	public void setArcWidth(int mArcWidth) {
		this.mArcWidth = mArcWidth;
		mArcPaint.setStrokeWidth(mArcWidth);
	}

	public int getInnerBounds() {
		return mBoundsInner;
	}

	public void setInnerBounds(int bounds) {
		mBoundsInner = bounds;
		measureBounds();
	}

	public int getOuterBounds() {
		return mBoundsOuter;
	}

	public void setOuterBounds(int bounds) {
		mBoundsOuter = bounds;
		measureBounds();
	}

	public int getArcRotation() {
		return mRotation;
	}

	public void setArcRotation(int mRotation) {
		this.mRotation = mRotation;
		updateThumbPosition();
	}

	public int getStartAngle() {
		return mStartAngle;
	}

	public void setStartAngle(int mStartAngle) {
		this.mStartAngle = mStartAngle;
		updateThumbPosition();
	}

	public int getSweepAngle() {
		return mSweepAngle;
	}

	public void setSweepAngle(int mSweepAngle) {
		this.mSweepAngle = mSweepAngle;
		updateThumbPosition();
	}

	public boolean isRoundedEdges() {
		return mRoundedEdges;
	}
	
	public void setRoundedEdges(boolean isEnabled) {
		mRoundedEdges = isEnabled;
		if (mRoundedEdges) {
			mArcPaint.setStrokeCap(Paint.Cap.ROUND);
			mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
			mProposedPaint.setStrokeCap(Paint.Cap.ROUND);
		} else {
			mArcPaint.setStrokeCap(Paint.Cap.SQUARE);
			mProgressPaint.setStrokeCap(Paint.Cap.SQUARE);
			mProposedPaint.setStrokeCap(Paint.Cap.SQUARE);
		}
	}

	public boolean isClockwise() {
		return mClockwise;
	}
	
	public void setClockwise(boolean isClockwise) {
		mClockwise = isClockwise;
	}

	public boolean isEnabled() {
		return mEnabled;
	}

	public void setEnabled(boolean enabled) {
		this.mEnabled = enabled;
	}

	public void setContinuous(boolean continuous) {
		mContinuous = continuous;

		if (continuous) {
			mSweepNormal = mSweepAngle;
			mSweepAngle = 360;
		}
		else {
			mSweepAngle = mSweepNormal;
		}
	}

	public int getProgressColor() {
		return mProgressPaint.getColor();
	}

	public void setProgressColor(int color) {
		mProgressPaint.setColor(color);
		invalidate();
	}

	public int getArcColor() {
		return mArcPaint.getColor();
	}

	public void setArcColor(int color) {
		mArcPaint.setColor(color);
		invalidate();
	}

	public int getMax() {
		return mMax;
	}

	public void setMax(int mMax) {
		this.mMax = mMax;
	}
}
