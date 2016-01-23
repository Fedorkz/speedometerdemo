/*
 * Copyright 2015 FedorkZ. 
 * 
 * Author      : Fedor Sokovikov
 */
package com.fedorkzsoft.speedometer;

import java.util.ArrayList;
import java.util.List;

import com.fedorkzsoft.speedometer.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Speedometer controll
 */
public class SpeedometerView extends SurfaceView implements IBlinkProvider {

    private static final int DBG_TEXT_SIZE = 50;
	private static final int DEF_SCALE_ACTIVE_FONT_SIZE = 70;
	private static final int DEF_SCALE_FONT_SIZE = 50;
	private static final int DEF_ARROW_LENGTH = 375;
	private static final int DEF_SCALE_DOTS_RADIUS = 370;
	private static final int DEF_SCALE_DIGITS_RADIUS = 400;
	private static final int DEGREE_MID = 90;
	private static final long FPS_CALC_INTERVAL = 1000L;
    private static final long INERTION_TIME = 300L;
	private static final float FLING_THRES = 0;
	private static final float SCALE_STEP = 10;
	private static final float SCALE_MIN = 0;
	private static final float SCALE_MAX = 180;
	
    private static final float ARROW_POINT_RATIO = 4f/5f;
	
	private static final String FONT = "cristal.ttf";
	
    private List<Segment> mSegments = new ArrayList<Segment>();

    private Bitmap bitmapGreen;
    private Bitmap bitmapBlue;
    private Bitmap bitmapYellow;
    private Bitmap bitmapRed;
    private Paint paint;
    private float mTargetValue = 0;
    private float mStartValue = 0;
    private float mCurValue = 0;
    

    private long lastFpsCalcUptime;
    private long frameCounter;

    private long fps;
	private int mArrowWidth;
	private int mArrowHeight;
	private Bitmap bitmapRedBlinker;
	private long mValueSetTime;
	
	Interpolator mInterpolator;
	private boolean mNeedUpdateValue;
	private Typeface mFont;
	private Paint mScalePaint;
	private Rect mRect;
	
	private int mScaleDigitsRadius = DEF_SCALE_DIGITS_RADIUS;
	private int mScaleDotsRadius = DEF_SCALE_DOTS_RADIUS;
	private int mArrowLength = DEF_ARROW_LENGTH;
	private int mScaleFontSize = DEF_SCALE_FONT_SIZE;
	private int mScaleFontActiveSize = DEF_SCALE_ACTIVE_FONT_SIZE;

    public SpeedometerView(Context context) {
        this(context, null);
    }

    public SpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        if (attrs != null)
        	readAttrs(context, attrs, 0);
    }
    
    public SpeedometerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(context, attrs, defStyle);
	}

    private void readAttrs(Context context, AttributeSet attrs, int defStyle) {
    	  TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.speedometer, defStyle, 0);

    	  mArrowLength = a.getDimensionPixelSize(R.styleable.speedometer_arrow_length, DEF_ARROW_LENGTH);
    	  mScaleDotsRadius = a.getDimensionPixelSize(R.styleable.speedometer_scale_dots_radius, DEF_SCALE_DOTS_RADIUS);
    	  mScaleDigitsRadius = a.getDimensionPixelSize(R.styleable.speedometer_scale_digits_radius, DEF_SCALE_DIGITS_RADIUS);
    	  mScaleFontSize = a.getDimensionPixelSize(R.styleable.speedometer_scale_font_size, DEF_SCALE_FONT_SIZE);
    	  mScaleFontActiveSize = a.getDimensionPixelSize(R.styleable.speedometer_scale_font_active_size, DEF_SCALE_ACTIVE_FONT_SIZE);

    	  a.recycle();
	}

	public void onResume() {
        prepare();
    }

    public void onPause() {
    }
    
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}


    @SuppressLint("NewApi")
    @Override
    protected void onDraw(Canvas canvas) {
        measureFps();
        int h = getHeight();
        int w = getWidth();
        
        updateValue();

        float value = mCurValue;

        paint.setAlpha(255);
        paint.setTextSize(DBG_TEXT_SIZE);
        canvas.drawColor(Color.BLACK);

        canvas.drawText("fps=" + fps, 0, DBG_TEXT_SIZE, paint);
        
        mScalePaint.setColor(Color.GREEN);
        mScalePaint.setTextSize(mScaleFontSize);

        Segment firstSegment = mSegments.get(0);
        Segment lastSegment = mSegments.get(mSegments.size()-1);

        if (value > lastSegment.maxAngle){
        	value = lastSegment.maxAngle;
        }

        if (value < firstSegment.minAngle){
        	value = firstSegment.minAngle;
        }

        drawScale(value, w /2, h - mScaleDigitsRadius + (int)(mArrowHeight * ARROW_POINT_RATIO), canvas);
        
        //Draw arrow
        canvas.rotate(value - 90, w /2, h - mScaleDigitsRadius + (int)(mArrowHeight * ARROW_POINT_RATIO));
        
        int x = w /2 - mArrowWidth /2;
        int y = h - mScaleDigitsRadius;

        for (Segment s: mSegments){
        	if ((s.minAngle <= value && value < s.maxAngle) ||
    			(lastSegment == s && s.minAngle <= value && value <= s.maxAngle) ){
        		
    			drawArrow(s, value, x, y, canvas, paint, this);
        		break;
        	}
        }
    }

	public void drawArrow(Segment s, float v, int x, int y, Canvas canvas, Paint paint, IBlinkProvider blinkProvider){
		Bitmap endBitmap = s.endBitmap;
		Bitmap startBitmap = s.startBitmap;
		Bitmap blinkBitmap = s.blinkBitmap;

		float minAngle = s.minAngle;
		float maxAngle = s.maxAngle;
		
		
		int alpha = (int) (255f * Util.countPrct(v, minAngle , maxAngle));

		if (s.endBitmap == null){
			paint.setAlpha(0xff);
			canvas.drawBitmap(startBitmap, x, y, paint);
		} else {
			paint.setAlpha(0xff - alpha);
			canvas.drawBitmap(startBitmap, x, y, paint);
			
			paint.setAlpha(alpha);
			canvas.drawBitmap(endBitmap, x, y, paint);
		}

		if (blinkBitmap != null){
			long time = System.currentTimeMillis();
			int blinkPrct = blinkProvider.getBlinkAlpha(time);

			paint.setAlpha(blinkPrct);
			canvas.drawBitmap(blinkBitmap, x, y, paint);
		}
	}
	
    private void drawScale(float v, int x, int y, Canvas canvas) {
    	float step = SCALE_STEP;
    	for (float tick=SCALE_MIN; tick<= SCALE_MAX; tick+=step){
    		int x1 = (int) (Math.cos(Math.toRadians(tick)) * mScaleDigitsRadius);
    		int y1 = (int) (Math.sin(Math.toRadians(tick)) * mScaleDigitsRadius);    

    		int x2 = (int) (Math.cos(Math.toRadians(tick)) * mScaleDotsRadius);
    		int y2 = (int) (Math.sin(Math.toRadians(tick)) * mScaleDotsRadius);    

            int textSizeDiff = mScaleFontActiveSize - mScaleFontSize;
    		if (v>=tick && v<tick+step){//ACTIVE TICK
    			float prct = Util.countPrct(v, tick+step ,tick);
        		mScalePaint.setColor(Color.RED);
				mScalePaint.setTextSize(mScaleFontSize + prct * textSizeDiff);
    		} else if (v>=tick-step && v<tick){//AFTER ACTIVE TICK
    			float prct = Util.countPrct(v, tick-step, tick);
        		mScalePaint.setColor(Color.GREEN);
	            mScalePaint.setTextSize(mScaleFontSize + prct * textSizeDiff);
    		} else {//PASSIVE TICK
	    		mScalePaint.setColor(Color.GREEN);
	            mScalePaint.setTextSize(mScaleFontSize);
    		}
            
            String txt = ""+(int)tick;
            
            int align = 0;// RIGHT ELEMENT BY DEFAULT TO RIGHT
            if (tick<DEGREE_MID){// LEFT ELEMENTS - SHIFT TO LEFT
            	mScalePaint.getTextBounds(txt, 0, txt.length(), mRect);
            	align = - mRect.width();
            } else if (tick == DEGREE_MID){// CENTER ELEMENT SHOULD HAVE CENTER ALIGN
            	align = - mRect.width() / 2;
            }
            
        	canvas.drawText(txt, x-x1 + align, y-y1 , mScalePaint);
        	canvas.drawCircle(x - x2, y - y2, 5, mScalePaint);
    	}
	}

	private void updateValue() {
    	if (!mNeedUpdateValue)
    		return;
    	
    	long tm = System.currentTimeMillis();
    	long dt = (tm - mValueSetTime);
    	if (dt <= 0)
    		return;
    	
    	if (dt >= INERTION_TIME){
    		mNeedUpdateValue = false;
    		dt = INERTION_TIME;
    	}
    	
    	float progress = (float)dt / INERTION_TIME;
    	
    	if (mInterpolator != null)
    		progress = mInterpolator.getInterpolation(progress); 
    	
		mCurValue = mStartValue + (mTargetValue - mStartValue) * progress;
	}

	private void measureFps() {
        frameCounter++;
        long now = SystemClock.uptimeMillis();
        long delta = now - lastFpsCalcUptime;
        if (delta > FPS_CALC_INTERVAL) {
            fps = frameCounter * FPS_CALC_INTERVAL / delta;

            frameCounter = 0;
            lastFpsCalcUptime = now;
        }
    }

    private void prepare() {
        //
		bitmapGreen = BitmapFactory.decodeResource(getResources(), R.drawable.ar_green);

    	int origWidth = bitmapGreen.getWidth(); 
    	int origHeight = bitmapGreen.getHeight();
    	float ratio = origHeight / origWidth;
        
    	int height = mArrowLength; 
        int width = (int) (mArrowLength / ratio);
        
        bitmapGreen = Bitmap.createScaledBitmap(bitmapGreen, width, height,true);

        bitmapBlue = BitmapFactory.decodeResource(getResources(), R.drawable.ar_blue);
        bitmapBlue = Bitmap.createScaledBitmap(bitmapBlue, width, height,true);
        bitmapYellow = BitmapFactory.decodeResource(getResources(), R.drawable.ar_yellow);
        bitmapYellow = Bitmap.createScaledBitmap(bitmapYellow, width, height,true);
        bitmapRed = BitmapFactory.decodeResource(getResources(), R.drawable.ar_red);
        bitmapRed = Bitmap.createScaledBitmap(bitmapRed, width, height,true);
        bitmapRedBlinker = BitmapFactory.decodeResource(getResources(), R.drawable.ar_red_big);
        bitmapRedBlinker = Bitmap.createScaledBitmap(bitmapRed, width, height,true);
        
        mArrowWidth = bitmapRed.getWidth();
        mArrowHeight = bitmapRed.getHeight();

        paint = new Paint();
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        
        lastFpsCalcUptime = SystemClock.uptimeMillis();
        frameCounter = 0;
        
        initSegments();
        initFonts();
        
        setInterpolator(new OvershootInterpolator());// make it physics like
    }
    
    private void initFonts() {
    	mFont = Typeface.createFromAsset(getContext().getAssets(), FONT); 
    	mScalePaint = new Paint();
    	mScalePaint.setTypeface(mFont);
    	
    	mRect = new Rect();
	}

    
    /** this is a STRUCT class to store arrow regions
     * 
     * @author FedorkZ
     *
     */
	static class Segment{// JUST A STRUCT so public
    	public float minAngle;
    	public float maxAngle;
    	public Bitmap startBitmap;
    	public Bitmap endBitmap;
		private Bitmap blinkBitmap;
    	
    	public Segment(float _minAngle, float _maxAngle, Bitmap _startBitmap, Bitmap _endBitmap, Bitmap _blinkBitmap){
    		minAngle = _minAngle;
    		maxAngle = _maxAngle;
    		startBitmap = _startBitmap;
    		endBitmap = _endBitmap;
    		blinkBitmap = _blinkBitmap;
    	}
    }
    
    void initSegments(){
    	mSegments.add(new Segment(0, 30, bitmapBlue, null, null));
    	mSegments.add(new Segment(30, 40, bitmapBlue, bitmapGreen, null));
    	mSegments.add(new Segment(40, 70, bitmapGreen, null, null));
    	mSegments.add(new Segment(70, 80, bitmapGreen, bitmapYellow, null));
    	mSegments.add(new Segment(80, 110, bitmapYellow, null, null));
    	mSegments.add(new Segment(110, 120, bitmapYellow, bitmapRed, null));
    	mSegments.add(new Segment(120, 140, bitmapRed, null, null));
    	mSegments.add(new Segment(120, 180, bitmapRed, null, bitmapRedBlinker));
    }
    
    /**
     * get Synced blinking
     */
	public int getBlinkAlpha(long tm){
		float phase = (float)(Util.getLongLow(tm)) / 100f;
		return (int)( (1 + Math.sin((phase)) ) * 255f/2f);
	}
    
	public void setValue(float f) {
		if (Math.abs(mCurValue - f) > FLING_THRES){
			mNeedUpdateValue = true;
			mStartValue = mCurValue;
			mValueSetTime = System.currentTimeMillis();
			mTargetValue = f;
		} else {
			mNeedUpdateValue = false;
			mTargetValue = f;
			mCurValue = f;
			mStartValue = f;
		}
	}
	
	public void setInterpolator(Interpolator i){
		mInterpolator = i;
	}
	
}
