package com.example.utils;

import java.util.Vector;

import android.graphics.Point;

import com.example.uniformTouch.Touch;

public class ExtractTouchUtils {
	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	
	public static void extract2Points(Vector<Point> pointer, Touch[] touch, int w, int h){
		Vector<Point> extractedPointer = new Vector<Point>();
		extractedPointer = extractTouchPoint(pointer, -1, w, h);
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
	}
	
	// Extract two touching points nearest to center of left and right part of display
	private static Vector<Point> extractTouchPoint(Vector<Point> pointer, int type, int w, int h){
		// if number of original touching points is under 2, exit.
		if(pointer.size() <= 1){
			return pointer;
		} else if(pointer.size() <= 2){
			Point p0 = pointer.elementAt(0);
			Point p1 = pointer.elementAt(1);
			if(p0.x > p1.x){
				pointer.set(LEFT, p1);
				pointer.set(RIGHT, p0);
			}
			return pointer;
		}
		
		Point center = new Point(w/2, h/2);
		int leftInnerIndex = 0;
		int leftInnerDistance = (int)Math.sqrt(Math.pow(w/2, 2) + Math.pow(h/2, 2));
		int rightInnerIndex = 0;
		int rightInnerDistance = (int)Math.sqrt(Math.pow(w/2, 2) + Math.pow(h/2, 2));
		
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
}