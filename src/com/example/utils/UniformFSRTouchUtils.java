package com.example.utils;

import android.util.Log;

import com.example.uniformTouch.Touch;

public class UniformFSRTouchUtils {
	// Debugging
	private static final String TAG = "Uniform FSR and Touch Utility";
	
	public static final int DATA_PER_TOUCH = 3;
	
	/* ------------------------------------------------------------------------------ */
	/**
	 * Touch coordinate data (d0,d1) and pressure data for 4 vertices of device (p0,p1,p2,p3)
	 * come separately, each from android device and arduino via bluetooth connection.
	 * And these information have to be uniformed to Touch (t0,t1) which contains touch coordinate and pressure
	 * in a class.
	 * The relation of position between coordinates and pressures are following, for example.
	 * The device has width * height size of display.
	 * 			w (width)
	 * 	p0------------------p1
	 * 	|	d0			 	|
	 * 	|			d1		| h (height)
	 * 	|					|
	 * 	p2------------------p3
	 * 
	 * The relation can be calculated with equilibrium of forces (a) and torques around superordinate vertices (b).
	 * In equations (b), there are some constraint conditions among p0-p3.
	 * 	(a) ƒ°pi = ƒ°ti.p
	 * 	(b) (1) p2*h + p3*h = t0.p*d0.y + t1.p*d1.y				(x axis around p0, p1 vertices)
	 * 		(2) p1*w + p3*w = t0.p*d0.x + t1.p*d1.x				(y axis around p0, p2 vertices)
	 * 		(3) p0*w + p2*w = t0.p*(w-d0.x) + t1.p*(w-d1.x)		(y axis around p1, p3 vertices)
	 * 		(4) p0*h + p1*h = t0.p*(h-d0.y) + t1.p*(h-d1.y)		(x axis around p2, p3 vertices)
	 * 
	 * And equations (a) and (b) are summarized to matrix equation below.
	 * |0 0 h h| |p0| 	|d0.y   d1.y  | 
	 * |0 w 0 w| |p1| = |d0.x   d1.x  | |t0.p|
	 * |w 0 w 0| |p2| 	|w-d0.x w-d1.x| |t1.p|
	 * |h h 0 0| |p3| 	|h-d0.y h-d1.y|
	 * |1 1 1 1| 		|1		1	  |
	 * 
	 * We represent these as following equation of matrix.
	 * A F = M T
	 */
	public static Touch[] calculatePressureOfEachTouchPoint(int[] data, float[] fsr, int w, int h){
		int length = data.length/DATA_PER_TOUCH; // Each touch coordinate contains 3 members, as x, y and action.
		
		if(length == 0 || fsr.length != 4) return null;
		
		switch(length){
		case 1:
			// When there are only 1 touching point, the pressure of it is simply the average of 4 pressures.
			float p = fsr[0]+fsr[1]+fsr[2]+fsr[3];
			Touch[] t = new Touch[1];
			t[0] = new Touch(data[0], data[1], p, true);
			return t;
			
		case 2:
			// To solve the equation of matrixes, the lease-squares method is taken
			// Pseudo-inverse-matrix for X+ is calculated by the following equation.
			// X^+ = (X^T X)^(-1) X^T
			// T = M^+ A F = ((X^T X)^(-1) X^T) A F
			float[][] m = new float[5][2];
			{
				m[0][0] = data[1]; 		m[0][1] = data[4];
				m[1][0] = data[0]; 		m[1][1] = data[3];
				m[2][0] = w - data[0]; 	m[2][1] = w - data[3];
				m[3][0] = h - data[1]; 	m[3][1] = h - data[4];
				m[4][0] = 1.f; 			m[4][1] = 1.f;
			}
			float[][] a = new float[5][4];
			{
				a[0][0] = 0.f; 	a[0][1] = 0.f; 	a[0][2] = h;	a[0][3] = h;
				a[1][0] = 0.f; 	a[1][1] = w; 	a[1][2] = 0.f; 	a[1][3] = w;
				a[2][0] = w; 	a[2][1] = 0.f;	a[2][2] = w; 	a[2][3] = 0.f;
				a[3][0] = h;	a[3][1] = h; 	a[3][2] = 0.f; 	a[3][3] = 0.f;
				a[4][0] = 1.f; 	a[4][1] = 1.f; 	a[4][2] = 1.f; 	a[4][3] = 1.f;
			}
			float[][] f = new float[fsr.length][1];
			{
				f[0][0] = fsr[0];
				f[1][0] = fsr[1];
				f[2][0] = fsr[2];
				f[3][0] = fsr[3];
			}
			float[][] pp = new float[2][1];
			try{
				pp = MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.inverseMatrix(
						MatrixUtils.multiplyMatrix4nF(
							MatrixUtils.transposeMatrix(m), m)),
									MatrixUtils.transposeMatrix(m)), a), f);
			} catch (NullPointerException e){
				int denominator = data[0]*data[4] - data[1]*data[3];
				if(denominator == 0){
					pp[0][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/2.f;
					pp[1][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/2.f;
				} else {
					pp[0][0] = (float)(data[4]*(fsr[1]+fsr[3])*w - data[3]*(fsr[2]+fsr[3])*h) / (float)denominator;
					pp[1][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3]) - pp[0][0];
				}
			}
			Touch[] tt = new Touch[2];
			tt[0] = new Touch(data[0], data[1], pp[0][0], true);
			tt[1] = new Touch(data[3], data[4], pp[1][0], true);
			return tt;
			
		case 0:
		default:
				return null;
		}
	}

	/* ------------------------------------------------------------------------------ */
	/**
	 * Touch coordinate data (d0,d1) and pressure data for 4 vertices of device (p0,p1,p2,p3)
	 * come separately, each from android device and arduino via bluetooth connection.
	 * And these information have to be uniformed to Touch (t0,t1) which contains touch coordinate and pressure in a class.
	 * Other than these, there is a point which support two android devices, f0.
	 * The relation of position among coordinates and pressures are following, for example.
	 * The device has width * height size of display.
	 * 			w (width)
	 * 	p0------------------p1
	 * 	|	d0			 	|
	 * 	|		  f0   d1	|  h (height)
	 * 	|					|
	 * 	p2------------------p3
	 * 
	 * The relation can be calculated with equilibrium of forces (a) and torques around superordinate vertices (b).
	 * In equations (b), there are some constraint conditions among p0-p3.
	 * 	(a) ƒ°pi = ƒ°ti.p + ƒ°fi.p
	 * 	(b) (1) p2*h + p3*h = t0.p*d0.y + t1.p*d1.y + f0.p*h/2				(x axis around p0, p1 vertices)
	 * 		(2) p1*w + p3*w = t0.p*d0.x + t1.p*d1.x + f0.p*w/2				(y axis around p0, p2 vertices)
	 * 		(3) p0*w + p2*w = t0.p*(w-d0.x) + t1.p*(w-d1.x)	+ f0.p*w/2		(y axis around p1, p3 vertices)
	 * 		(4) p0*h + p1*h = t0.p*(h-d0.y) + t1.p*(h-d1.y) + f0.p*h/2		(x axis around p2, p3 vertices)
	 * 
	 * And equations (b) is summarized to matrix equation below.
	 * |0 0 h h| |p0| 	|d0.y   d1.y   h/2|
	 * |0 w 0 w| |p1| = |d0.x   d1.x   w/2| |t0.p|
	 * |w 0 w 0| |p2| 	|w-d0.x w-d1.x w/2| |t1.p|
	 * |h h 0 0| |p3| 	|h-d0.y h-d1.y h/2| |f0.p|
	 * |1 1 1 1| 		|1		1	   1  |
	 * 
	 * We represent this as following.
	 * A F = M T
	 */
	public static Touch[] calculatePressureOfEachTouchWithOneSupport(int[] data, float[] fsr, float w, float h){
		int length = data.length/DATA_PER_TOUCH; // Each touch coordinate contains 3 members, as x, y and action.
		
		// If touch point and fsr do not meet the length condition, return null.
		if(length == 0 || fsr.length != 4) return null;

		float[][] a = new float[5][4];
		{
			a[0][0] = 0.f; 	a[0][1] = 0.f; 	a[0][2] = h;	a[0][3] = h;
			a[1][0] = 0.f; 	a[1][1] = w; 	a[1][2] = 0.f; 	a[1][3] = w;
			a[2][0] = w; 	a[2][1] = 0.f;	a[2][2] = w; 	a[2][3] = 0.f;
			a[3][0] = h;	a[3][1] = h; 	a[3][2] = 0.f; 	a[3][3] = 0.f;
			a[4][0] = 1.f; 	a[4][1] = 1.f; 	a[4][2] = 1.f; 	a[4][3] = 1.f;
		}
		float[][] f = new float[fsr.length][1];
		{
			f[0][0] = fsr[0];
			f[1][0] = fsr[1];
			f[2][0] = fsr[2];
			f[3][0] = fsr[3];
		}
		
		switch(length){
		case 1:
			float[][] m2 = new float[5][2];
			{
				m2[0][0] = data[1]; 		m2[0][1] = h/2.f;
				m2[1][0] = data[0]; 		m2[1][1] = w/2.f;
				m2[2][0] = w - data[0]; 	m2[2][1] = w/2.f;
				m2[3][0] = h - data[1]; 	m2[3][1] = h/2.f;
				m2[4][0] = 1.f; 			m2[4][1] = 1.f;
			}
			float[][] p2 = new float[2][1];
			try{
				p2 = MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.inverseMatrix(
						MatrixUtils.multiplyMatrix4nF(
							MatrixUtils.transposeMatrix(m2), m2)),
									MatrixUtils.transposeMatrix(m2)), a), f);
			} catch (NullPointerException e){
				float denominator2x = 2*data[0] - w;
				if(denominator2x != 0){
					p2[0][0] = (fsr[1]+fsr[3]-fsr[0]-fsr[2])*w / denominator2x;
				} else {
					float denominator2y = 2*data[1] - h;
					if(denominator2y != 0){
						p2[0][0] = (fsr[2]+fsr[3]-fsr[0]-fsr[1])*h / denominator2y;
					} else{
						p2[0][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/2.f;
					}
				}
			}
			Touch[] t = new Touch[1];
			t[0] = new Touch(data[0], data[1], p2[0][0], true);
			return t;
			
		case 2:
			float[][] m3 = new float[5][3];
			{
				m3[0][0] = data[1]; 		m3[0][1] = data[4];		m3[0][2] = h/2.f;
				m3[1][0] = data[0]; 		m3[1][1] = data[3];		m3[1][2] = w/2.f;
				m3[2][0] = w - data[0]; 	m3[2][1] = w - data[3];	m3[2][2] = w/2.f;
				m3[3][0] = h - data[1]; 	m3[3][1] = h - data[4];	m3[3][2] = h/2.f;
				m3[4][0] = 1.f; 			m3[4][1] = 1.f; 		m3[4][2] = 1.f;
			}
			float[][] p3 = new float[3][1];
			try{
				p3 = MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.inverseMatrix(
						MatrixUtils.multiplyMatrix4nF(
							MatrixUtils.transposeMatrix(m3), m3)),
									MatrixUtils.transposeMatrix(m3)), a), f);
			} catch (NullPointerException e){
				// TODO
				// This approach is not correct. Fix it.
				p3[0][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/3.f;
				p3[1][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/3.f;
				// TODO
			}
			Touch[] tt = new Touch[2];
			tt[0] = new Touch(data[0], data[1], p3[0][0], true);
			tt[1] = new Touch(data[3], data[4], p3[1][0], true);
			return tt;
			
		case 0:
		default:
				return null;
		}
	}

	/* ------------------------------------------------------------------------------ */
	/**
	 * Touch coordinate data (d0,d1) and pressure data for 4 vertices of device (p0,p1,p2,p3)
	 * come separately, each from android device and arduino via bluetooth connection.
	 * And these information have to be uniformed to Touch (t0,t1) which contains touch coordinate and pressure
	 * in a class.
	 * Other than these, there are two points which attaches two android devices, (f0, f1).
	 * The relation of position among coordinates and pressures are following, for example.
	 * The device has width * height size of display.
	 * 			w (width)
	 * 	p0------------------p1
	 * 	|	d0			 	|
	 * 	f0			d1		f1 h (height)
	 * 	|					|
	 * 	p2------------------p3
	 * 
	 * The relation can be calculated with equilibrium of forces (a) and torques around superordinate vertices (b).
	 * In equations (b), there are some constraint conditions among p0-p3.
	 * 	(a) ƒ°pi = ƒ°ti.p + ƒ°fi.p
	 * 	(b) (1) p2*h + p3*h = t0.p*d0.y + t1.p*d1.y + f0.p*h/2 + f1.p*h/2				(x axis around p0, p1 vertices)
	 * 		(2) p1*w + p3*w = t0.p*d0.x + t1.p*d1.x + f1.p*w							(y axis around p0, p2 vertices)
	 * 		(3) p0*w + p2*w = t0.p*(w-d0.x) + t1.p*(w-d1.x)	+ f0.p*w					(y axis around p1, p3 vertices)
	 * 		(4) p0*h + p1*h = t0.p*(h-d0.y) + t1.p*(h-d1.y) + f0.p*h/2 + f1.p*h/2		(x axis around p2, p3 vertices)
	 * 
	 * And equations (b) is summarized to matrix equation below.
	 * |0 0 h h| |p0| 	|d0.y   d1.y   h/2 	h/2| |t0.p|
	 * |0 w 0 w| |p1| = |d0.x   d1.x   0	w  | |t1.p|
	 * |w 0 w 0| |p2| 	|w-d0.x w-d1.x w	0  | |f0.p|
	 * |h h 0 0| |p3| 	|h-d0.y h-d1.y h/2	h/2| |f1.p|
	 * |1 1 1 1| 		|1		1	   1	1  |
	 * 
	 * We represent this as following.
	 * A F = M T
	 */
	public static Touch[] calculatePressureOfEachTouchWithTwoSupport(int[] data, float[] fsr, float w, float h){
		int length = data.length/DATA_PER_TOUCH; // Each touch coordinate contains 3 members, as x, y and action.
		
		if(length == 0 || fsr.length != 4) return null;

		switch(length){
		case 1:
			// TODO
			// This approach is not correct. Fix it.
			// When there are only 1 touching point, the pressure of it is simply the average of 4 pressures.
			float p = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/3.f;
			Touch[] t = new Touch[1];
			t[0] = new Touch(data[0], data[1], p, true);
			// TODO
			return t;
			
		case 2:
			float[][] m = new float[5][4];
			{
				m[0][0] = data[1]; 		m[0][1] = data[4];		m[0][2] = h/2.f;	m[0][3] = h/2.f;
				m[1][0] = data[0]; 		m[1][1] = data[3];		m[1][2] = 0.f;		m[1][3] = w;
				m[2][0] = w - data[0]; 	m[2][1] = w - data[3];	m[2][2] = w;		m[2][3] = 0.f;
				m[3][0] = h - data[1]; 	m[3][1] = h - data[4];	m[3][2] = h/2.f;	m[3][3] = h/2.f;
				m[4][0] = 1.f; 			m[4][1] = 1.f; 			m[4][2] = 1.f; 		m[4][3] = 1.f;
			}
			float[][] a = new float[5][4];
			{
				a[0][0] = 0.f; 	a[0][1] = 0.f; 	a[0][2] = h;	a[0][3] = h;
				a[1][0] = 0.f; 	a[1][1] = w; 	a[1][2] = 0.f; 	a[1][3] = w;
				a[2][0] = w; 	a[2][1] = 0.f;	a[2][2] = w; 	a[2][3] = 0.f;
				a[3][0] = h;	a[3][1] = h; 	a[3][2] = 0.f; 	a[3][3] = 0.f;
				a[4][0] = 1.f; 	a[4][1] = 1.f; 	a[4][2] = 1.f; 	a[4][3] = 1.f;
			}
			float[][] f = new float[fsr.length][1];
			{
				f[0][0] = fsr[0];
				f[1][0] = fsr[1];
				f[2][0] = fsr[2];
				f[3][0] = fsr[3];
			}
			float[][] pp = new float[4][1];
			try{
				pp = MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.inverseMatrix(
						MatrixUtils.multiplyMatrix4nF(
							MatrixUtils.transposeMatrix(m), m)),
									MatrixUtils.transposeMatrix(m)), a), f);
			} catch (NullPointerException e){
				// TODO
				// This approach is not correct. Fix it.
				pp[0][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/4.f;
				pp[1][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/4.f;
				// TODO
			}
			Touch[] tt = new Touch[2];
			tt[0] = new Touch(data[0], data[1], pp[0][0], true);
			tt[1] = new Touch(data[3], data[4], pp[1][0], true);
			return tt;
			
		case 0:
		default:
				return null;
		}
	}

	/* ------------------------------------------------------------------------------ */
	/**
	 * Touch coordinate data (d0,d1) and pressure data for 4 vertices of device (p0,p1,p2,p3)
	 * come separately, each from android device and arduino via bluetooth connection.
	 * And these information have to be uniformed to Touch (t0,t1) which contains touch coordinate and pressure
	 * in a class.
	 * Other than these, there are three points which attaches two android devices, (f0, f1, f2).
	 * The relation of position among coordinates and pressures are following, for example.
	 * The device has width * height size of display.
	 * 			w (width)
	 * 	p0------------------p1
	 * 	|	d0			 	|
	 * 	f0		  f2 d1		f1 h (height)
	 * 	|					|
	 * 	p2------------------p3
	 * 
	 * The relation can be calculated with equilibrium of forces (a) and torques around superordinate vertices (b).
	 * In equations (b), there are some constraint conditions among p0-p3.
	 * 	(a) ƒ°pi = ƒ°ti.p + ƒ°fi.p
	 * 	(b) (1) p2*h + p3*h = t0.p*d0.y + t1.p*d1.y + f0.p*h/2 + f1.p*h/2 + f2.p*h/2			(x axis around p0, p1 vertices)
	 * 		(2) p1*w + p3*w = t0.p*d0.x + t1.p*d1.x + f1.p*w + f2.p*w/2							(y axis around p0, p2 vertices)
	 * 		(3) p0*w + p2*w = t0.p*(w-d0.x) + t1.p*(w-d1.x)	+ f0.p*w + f2.p*w/2					(y axis around p1, p3 vertices)
	 * 		(4) p0*h + p1*h = t0.p*(h-d0.y) + t1.p*(h-d1.y) + f0.p*h/2 + f1.p*h/2 + f2.p*h/2	(x axis around p2, p3 vertices)
	 * 
	 * And equations (b) is summarized to matrix equation below.
	 * |0 0 h h| |p0| 	|d0.y   d1.y   h/2 	h/2	h/2| |t0.p|
	 * |0 w 0 w| |p1| = |d0.x   d1.x   0	w   w/2| |t1.p|
	 * |w 0 w 0| |p2| 	|w-d0.x w-d1.x w	0   w/2| |f0.p|
	 * |h h 0 0| |p3| 	|h-d0.y h-d1.y h/2	h/2	h/2| |f1.p|
	 * |1 1 1 1| 		|1		1	   1	1   1  | |f2.p|
	 * 
	 * We represent this as following.
	 * A F = M T
	 */
	public static Touch[] calculatePressureOfEachTouchWithThreeSupport(int[] data, float[] fsr, float w, float h){
		int length = data.length/DATA_PER_TOUCH; // Each touch coordinate contains 3 members, as x, y and action.
		
		// If touch point and fsr do not meet the length condition, return null.
		if(length == 0 || fsr.length != 4) return null;

		float[][] a = new float[5][4];
		{
			a[0][0] = 0.f; 	a[0][1] = 0.f; 	a[0][2] = h;	a[0][3] = h;
			a[1][0] = 0.f; 	a[1][1] = w; 	a[1][2] = 0.f; 	a[1][3] = w;
			a[2][0] = w; 	a[2][1] = 0.f;	a[2][2] = w; 	a[2][3] = 0.f;
			a[3][0] = h;	a[3][1] = h; 	a[3][2] = 0.f; 	a[3][3] = 0.f;
			a[4][0] = 1.f; 	a[4][1] = 1.f; 	a[4][2] = 1.f; 	a[4][3] = 1.f;
		}
		float[][] f = new float[fsr.length][1];
		{
			f[0][0] = fsr[0];
			f[1][0] = fsr[1];
			f[2][0] = fsr[2];
			f[3][0] = fsr[3];
		}
		
		switch(length){
		case 1:
			// TODO
			// This approach is not correct. Fix it.
			// When there are only 1 touching point, the pressure of it is simply the average of 4 pressures.
			float p = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/4.f;
			Touch[] t = new Touch[1];
			t[0] = new Touch(data[0], data[1], p, true);
			// TODO
			return t;
			
		case 2:
			float[][] m5 = new float[5][5];
			{
				m5[0][0] = data[1]; 		m5[0][1] = data[4];		m5[0][2] = h/2.f;	m5[0][3] = h/2.f;	m5[0][4] = h/2.f;
				m5[1][0] = data[0]; 		m5[1][1] = data[3];		m5[1][2] = 0.f;		m5[1][3] = w;		m5[1][4] = w/2.f;
				m5[2][0] = w - data[0]; 	m5[2][1] = w - data[3];	m5[2][2] = w;		m5[2][3] = 0.f;		m5[2][4] = w/2.f;
				m5[3][0] = h - data[1]; 	m5[3][1] = h - data[4];	m5[3][2] = h/2.f;	m5[3][3] = h/2.f;	m5[3][4] = h/2.f;
				m5[4][0] = 1.f; 			m5[4][1] = 1.f; 		m5[4][2] = 1.f; 	m5[4][3] = 1.f;		m5[4][4] = 1.f;
			}
			float[][] pp5 = new float[5][1];
			try{
				pp5 = MatrixUtils.multiplyMatrix4nF(
						MatrixUtils.multiplyMatrix4nF(
									MatrixUtils.inverseMatrix(m5), a), f);
			} catch (NullPointerException e){
				// TODO
				// This approach is not correct. Fix it.
				pp5[0][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/5.f;
				pp5[1][0] = (fsr[0]+fsr[1]+fsr[2]+fsr[3])/5.f;
				// TODO
			}
			Touch[] tt = new Touch[2];
			tt[0] = new Touch(data[0], data[1], pp5[0][0], true);
			tt[1] = new Touch(data[3], data[4], pp5[1][0], true);
			return tt;
			
		case 0:
		default:
				return null;
		}
	}
}