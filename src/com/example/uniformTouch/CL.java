package com.example.uniformTouch;

import android.graphics.RectF;

/**
 * 
 * @author art
 * Canvas Layout constant values
 */
public class CL {
	// Hidden Command
	public static final RectF H00 = new RectF(0.f, 0.f, .2f, .2f);
	public static final RectF H01 = new RectF(.2f, 0.f, .4f, .2f);
	public static final RectF H02 = new RectF(.4f, 0.f, .6f, .2f);
	public static final RectF H03 = new RectF(.6f, 0.f, .8f, .2f);
	public static final RectF H04 = new RectF(.8f, 0.f, 1.f, .2f);
	
	public static final RectF H10 = new RectF(0.f, .2f, .2f, .4f);
	public static final RectF H11 = new RectF(.2f, .2f, .4f, .4f);
	public static final RectF H12 = new RectF(.4f, .2f, .6f, .4f);
	public static final RectF H13 = new RectF(.6f, .2f, .8f, .4f);
	public static final RectF H14 = new RectF(.8f, .2f, 1.f, .4f);
	
	public static final RectF H20 = new RectF(0.f, .4f, .2f, .6f);
	public static final RectF H21 = new RectF(.2f, .4f, .4f, .6f);
	public static final RectF H22 = new RectF(.4f, .4f, .6f, .6f);
	public static final RectF H23 = new RectF(.6f, .4f, .8f, .6f);
	public static final RectF H24 = new RectF(.8f, .4f, 1.f, .6f);
	
	public static final RectF H30 = new RectF(0.f, .6f, .2f, .8f);
	public static final RectF H31 = new RectF(.2f, .6f, .4f, .8f);
	public static final RectF H32 = new RectF(.4f, .6f, .6f, .8f);
	public static final RectF H33 = new RectF(.6f, .6f, .8f, .8f);
	public static final RectF H34 = new RectF(.8f, .6f, 1.f, .8f);

	public static final RectF H40 = new RectF(0.f, .8f, .2f, 1.f);
	public static final RectF H41 = new RectF(.2f, .8f, .4f, 1.f);
	public static final RectF H42 = new RectF(.4f, .8f, .6f, 1.f);
	public static final RectF H43 = new RectF(.6f, .8f, .8f, 1.f);
	public static final RectF H44 = new RectF(.8f, .8f, 1.f, 1.f);
	
	
	// Surface View
	public static final int NEXUS7_WIDTH = 1920;
	public static final int NEXUS7_HEIGHT = 1080;
	public static final int MODEL_OFFSET_X = NEXUS7_WIDTH/4;
	public static final int MODEL_OFFSET_Y = NEXUS7_HEIGHT/4;
	public static final int MODEL_WIDTH = NEXUS7_WIDTH - MODEL_OFFSET_X*2;
	public static final int MODEL_HEIGHT = NEXUS7_HEIGHT - MODEL_OFFSET_Y*2;
	
	// Graphics Size of Parts 
	public static final float RADIUS = 50.f;
	public static final float POINT = 5.f;
	public static final float STROKE_W = 2.f;
	
	public static final float TEXT_SIZE_S = 18.f;
	public static final float TEXT_SIZE_M = 28.f;
	public static final float TEXT_SIZE_L = 38.f;
}
