package com.example.touchinfo;

public class TouchRow {
	private int coordinate_x;
	private int coordinate_y;

	public TouchRow(int coordinate_x, int coordinate_y) {
		this.coordinate_x = coordinate_x;
		this.coordinate_y = coordinate_y;
	}
	
	public TouchRow(){
		coordinate_x = 0;
		coordinate_y = 0;
	}

	public int getCoordinateX(){
		return coordinate_x;
	}
	
	public int getCoordinateY(){
		return coordinate_y;
	}
}