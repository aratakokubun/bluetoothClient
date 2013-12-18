package com.example.touchinfo;

public class FSRRow {
	private float p;

	public FSRRow(float p) {
		this.p = p;
	}
	
	public FSRRow(){
		this.p = 0.f;
	}

	public float getPressure(){
		return p;
	}
}