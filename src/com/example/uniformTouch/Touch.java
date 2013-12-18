package com.example.uniformTouch;

public class Touch {
	private int x;
	private int y;
	private float p;
	private boolean enabled;
	
	public Touch(){
		x = 0;
		y = 0;
		p = 0.f;
		enabled = false;
	}
	
	public Touch(int x, int y, float p, boolean enabled){
		this.x = x;
		this.y = y;
		this.p = p;
		this.enabled = enabled;
	}
	
	/* ---------------------------------------------- */
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public float getP(){
		return p;
	}
	
	public boolean getEnabled(){
		return enabled;
	}
	
	/* ---------------------------------------------- */
	public void setX(int x){
		this.x = x;
	}
	
	public void setY(int y){
		this.y = y;
	}
	
	public void setP(float p){
		this.p = p;
	}
	
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}
}
