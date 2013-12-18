package com.example.uniformTouch;

import java.util.Vector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.bluetoothclient.Receiver;
import com.example.utils.SensorCalibUtilsF;
import com.example.utils.UniformFSRTouchUtils;

public class UniformTouchSurface extends SurfaceView implements SurfaceHolder.Callback {
	// Debugging
	private static final String TAG = "Uniform Touch Surface";
	// FPS
	public static final long FPS = 50l;
	public static final long FRAME_TIME = 1000l/FPS;		

	private Handler mHandler = new Handler();
	private boolean isRunnable = true;
	
	private int width;
	private int height;
	
	private float[] fsr = new float[Receiver.FSR_NUM];
	private Touch[] touch = new Touch[2]; // This size have to be changed according to the format size of touch
	
	// Detection of hidden command
	private static final long TIME_BETWEEN_DOUBLE_TOUCH = 500;
	long lastTime = 0;
	private Rect sensorCalibArea;

	/* ------------------------------------------------------------------------------- */
	public UniformTouchSurface(Context context, int w, int h){
		super(context);
		
		initMembers(w, h);
		
		// Start Tread
		(new Thread(new Runnable(){
			@Override
			public void run(){
				long nowTime = System.currentTimeMillis();
				long lastTime = System.currentTimeMillis();
				long waitTime = 0l;
				
				while(isRunnable){
					nowTime = System.currentTimeMillis();
					waitTime = FRAME_TIME - (nowTime - lastTime);
					
					if(waitTime > 0l){
						try{
							Thread.sleep(waitTime);
						} catch(InterruptedException e){
						}
					}
					lastTime = nowTime;
				
					mHandler.post(new Runnable(){
						@Override
						public void run(){
							handleTouchMessage();
							
							invalidate();
						}
					});
				}
			}
		})).start();
	}
	
	private void initMembers(int w, int h){
		width = w;
		height = h;
		
		for(int i = 0; i < fsr.length; i++){
			fsr[i] = 0.f;
		}
		for(int i = 0; i < touch.length; i++){
			touch[i] = new Touch();
		}
		
		lastTime = System.currentTimeMillis();
		sensorCalibArea = new Rect((int)(width*CL.H04.left), (int)(height*CL.H04.top), (int)(width*CL.H04.right), (int)(height*CL.H04.bottom));

		setBackgroundColor(Color.WHITE);
	}
	
	/* ------------------------------------------------------------------------------- */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas){		
		Paint paint = new Paint();
		
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(CL.STROKE_W);
		{
			canvas.drawLine(
					CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y,
					CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y,
					paint);
			canvas.drawLine(
					CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y,
					CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT,
					paint);
			canvas.drawLine(
					CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y,
					CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT,
					paint);
			canvas.drawLine(
					CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT,
					CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT,
					paint);
		}
		
		paint.setColor(Color.RED);
		paint.setTextSize(CL.TEXT_SIZE_M);
		{
			canvas.drawCircle(
					CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y,
					fsr[0]*CL.RADIUS, paint);
			canvas.drawCircle(
					CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y,
					fsr[1]*CL.RADIUS, paint);
			canvas.drawCircle(
					CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT,
					fsr[2]*CL.RADIUS, paint);
			canvas.drawCircle(
					CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT,
					fsr[3]*CL.RADIUS, paint);
		}
		paint.setColor(Color.BLACK);
		{
			canvas.drawText(String.valueOf(fsr[0]), CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y, paint);
			canvas.drawText(String.valueOf(fsr[1]), CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y, paint);
			canvas.drawText(String.valueOf(fsr[2]), CL.MODEL_OFFSET_X, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT, paint);
			canvas.drawText(String.valueOf(fsr[3]), CL.MODEL_OFFSET_X+CL.MODEL_WIDTH, CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT, paint);
		}
		
		paint.setTextSize(CL.TEXT_SIZE_M);
		for(int i = 0; i < touch.length; i++){
			if(touch[i].getEnabled()){
				float radius = touch[i].getP()*CL.RADIUS;
				paint.setColor(Color.GREEN);
				canvas.drawCircle(
						CL.MODEL_OFFSET_X + (int)(CL.MODEL_WIDTH*(touch[i].getX() / (float)CL.NEXUS7_WIDTH)),
						CL.MODEL_OFFSET_Y + (int)(CL.MODEL_HEIGHT*(touch[i].getY() / (float)CL.NEXUS7_HEIGHT)),
						radius < CL.POINT ? CL.POINT : radius, paint);
				paint.setColor(Color.BLACK);
				canvas.drawText(
						String.valueOf(touch[i].getP()),
						CL.MODEL_OFFSET_X + (int)(CL.MODEL_WIDTH*(touch[i].getX() / (float)CL.NEXUS7_WIDTH)),
						CL.MODEL_OFFSET_Y + (int)(CL.MODEL_HEIGHT*(touch[i].getY() / (float)CL.NEXUS7_HEIGHT)),
						paint);
			}
		}
	}
	
	/* ------------------------------------------------------------------------------- */
	public boolean setFsr(float[] f){
		if(fsr.length != f.length) return false;
		
		// Map reference to each FSRs
		/*
		// From 0 1 2 3
		// To   1 0 3 2
		this.fsr[0]	= f[1];
		this.fsr[1] = f[0];
		this.fsr[2] = f[3];
		this.fsr[3] = f[2];
		*/
		// From 0 1 2 3
		// To 	0 1 2 3
		this.fsr[0] = f[0];
		this.fsr[1] = f[1];
		this.fsr[2] = f[2];
		this.fsr[3] = f[3];
		
		return true;
	}
	
	public boolean setTouch(Touch[] t){
		if(touch.length != t.length) return false;
		
		for(int i = 0; i < touch.length; i++){
			touch[i] = t[i];
		}
		
		return true;
	}
	
	/* ------------------------------------------------------------------------------ */
	protected void handleTouchMessage(){
		// Count enabled touch points
		// TODO two loops are not required. Use vector.
		int count = 0;
		for (int i = 0; i < touch.length; i++){
			if(touch[i].getEnabled()) count++;
		}
		
		// Reconstruct touch data array[integer]
		if(count == 0) return;
		
		int data[] = new int[count * UniformFSRTouchUtils.DATA_PER_TOUCH];
		int num = 0;
		for (int i = 0; i < touch.length; i++) {
			if(touch[i].getEnabled()){
				int index = num * UniformFSRTouchUtils.DATA_PER_TOUCH;
				data[index] = touch[i].getX();
				data[index + 1] = touch[i].getY();
				data[index + 2] = 0;
				num++;
			}
		}
		
		Touch[] t = UniformFSRTouchUtils.calculatePressureOfEachTouchPoint(data, fsr, width, height);
		// Touch[] t = UniformFSRTouchUtils.calculatePressureOfEachTouchWithOneSupport(data, fsr, width, height);
		
		if(t == null){
			for(int i = 0; i < 2; i++){
				touch[i].setEnabled(false);
			}
		} else {
			int size = t.length;
			
			if(size == 1){
				int index = (t[0].getX() < width/2) ? 0 : 1;
				touch[index] = t[0];
				touch[1-index].setEnabled(false);
			} else {
				int leftIndex = (t[0].getX() < t[1].getX()) ? 0 : 1;
				touch[0] = t[leftIndex];
				touch[1] = t[1-leftIndex];
			}
		}
	}
		
	/* ------------------------------------------------------------------------------ */
	// When this method is called, the on touch event of activity is called.
	@Override
	public boolean onTouchEvent(MotionEvent event){
		int pointerCount = event.getPointerCount();
		Vector<Point> pointer = new Vector<Point>();
		
		/*
		// get multiple touch count
		for(int i = 0; i < pointerCount; i++){
			int x = (int)event.getX(i);
			int y = (int)event.getY(i);
			
			if( x >= CL.MODEL_OFFSET_X && x <= CL.MODEL_OFFSET_X+CL.MODEL_WIDTH &&
				y >= CL.MODEL_OFFSET_Y && y <= CL.MODEL_OFFSET_Y+CL.MODEL_HEIGHT ){
					Point p = new Point();
					// Use Landscape
					p.x = (int)((x - CL.MODEL_OFFSET_X)*(CL.NEXUS7_WIDTH / (float)CL.MODEL_WIDTH));
					p.y = (int)((y - CL.MODEL_OFFSET_Y)*(CL.NEXUS7_HEIGHT / (float)CL.MODEL_HEIGHT));
					// Use Portrait
					// p.x = (int)event.getX(i);
					// p.y = height - (int)event.getY(i);
					pointer.add(p);
			}
		}

		ExtractTouchUtils.extract2Points(pointer, touch, width, height);
		*/
		
		/*
		// extract 2 point from vector
		Vector<Point> extractedPointer = new Vector<Point>();
		extractedPointer = extractTouchPoint(pointer, -1);
		// create integer array for sending message. array[left, right]
		for(int i = 0; i < 2; i++){
			if(i < extractedPointer.size()){
				touch[i].setX(extractedPointer.elementAt(i).x);
				touch[i].setY(extractedPointer.elementAt(i).y);
				touch[i].setP(0.f);
				touch[i].setEnabled(true);
			} else {
				touch[i].setEnabled(false);
			}
		}
		*/
		
		// If touch is outside of model, detect hidden command
		if(pointer.size() == 0){
			int action = event.getAction();
			int x = (int)event.getX();
			int y = (int)event.getY();
			long nowTime = System.currentTimeMillis();
			
			if(action == MotionEvent.ACTION_UP){
				if(nowTime - lastTime < TIME_BETWEEN_DOUBLE_TOUCH){
					if(sensorCalibArea.left < x && sensorCalibArea.right > x
							&& sensorCalibArea.top < y && sensorCalibArea.bottom > y){
						SensorCalibUtilsF.setIsUseDynamic();
						((UniformTouchVisualization)getContext()).setCalib(true);
					}
				}
				
				lastTime = nowTime;
			}		
		}

		return ((UniformTouchVisualization)getContext()).onTouchEvent(event);
	}

	/*
	// Extract two touching points nearest to center of left and right part of display
	private Vector<Point> extractTouchPoint(Vector<Point> pointer, int type){
		// if number of original touching points is under 2, exit.
		if(pointer.size() <= 1){
			return pointer;
		} else if(pointer.size() <= 2){
			Point p0 = pointer.elementAt(0);
			Point p1 = pointer.elementAt(1);
			if(p0.x > p1.x){
				pointer.set(0, p1);
				pointer.set(1, p0);
			}
			return pointer;
		}
		
		Point center = new Point(width/2, height/2);
		int leftInnerIndex = 0;
		int leftInnerDistance = (int)Math.sqrt(Math.pow(width/2, 2) + Math.pow(height/2, 2));
		int rightInnerIndex = 0;
		int rightInnerDistance = (int)Math.sqrt(Math.pow(width/2, 2) + Math.pow(height/2, 2));
		
		if(type < 0){
			// x
			for(int i = 0; i < pointer.size(); i++){
				Point p = pointer.elementAt(i);
				int distance = center.x-p.x;
				if(distance>0 && distance<leftInnerDistance){
					leftInnerIndex = i;
					leftInnerDistance = distance;
				} else if(distance<0 && Math.abs(distance)<rightInnerDistance){
					rightInnerIndex = i;
					rightInnerDistance = Math.abs(distance);
				}
			}
		} else if(type > 0){
			// y
			for(int i = 0; i < pointer.size(); i++){
				Point p = pointer.elementAt(i);
				int distance = center.y-p.y;
				if(center.x-p.x>0 && distance<leftInnerDistance){
					leftInnerIndex = i;
					leftInnerDistance = distance;
				} else if(center.x-p.x<0 && Math.abs(distance)<rightInnerDistance){
					rightInnerIndex = i;
					rightInnerDistance = Math.abs(distance);
				}
			}
		} else {
			// x, y
			for(int i = 0; i < pointer.size(); i++){
				Point p = pointer.elementAt(i);
				int distance = (int)Math.sqrt(Math.pow(center.x-p.x, 2) + Math.pow(center.y-p.y, 2));
				if(center.x-p.x>0 && distance<leftInnerDistance){
					leftInnerIndex = i;
					leftInnerDistance = distance;
				} else if(center.x-p.x<0 && Math.abs(distance)<rightInnerDistance){
					rightInnerIndex = i;
					rightInnerDistance = Math.abs(distance);
				}
			}
		}
		
		// Create new vector includes inner pointer of left and right touching point
		Vector<Point> createdPointer = new Vector<Point>();
		createdPointer.add(pointer.elementAt(leftInnerIndex));
		createdPointer.add(pointer.elementAt(rightInnerIndex));
		
		return createdPointer;
	}
	*/

	/* ------------------------------------------------------------------------------- */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}

	public void killThread(){
		isRunnable = false;
	}
}