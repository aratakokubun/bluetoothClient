package com.example.touchinfo;

import java.util.ArrayList;

import android.graphics.Color;
import android.graphics.Point;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.bluetoothclient.R;
import com.example.bluetoothclient.Receiver;

public class TouchInfoController extends Receiver{
	// Debugging
	private static final String TAG = "TouchInfoController";
	
	// list view members
	private static ListView touchListView;
	private static ListView fsrListView;
	// array list members
	private static ArrayList<TouchRow> touchList;
	private static ArrayList<FSRRow> fsrList;
	// adapter members
	private static TouchRowAdapter touchAdapter;
	private static FSRRowAdapter fsrAdapter;
	
	public TouchInfoController(){
		super();
	}

	public void initView(){
		touchList = new ArrayList<TouchRow>();
		touchListView = (ListView) findViewById(R.id.touchList);
		touchAdapter = new TouchRowAdapter(this, R.layout.touch_row, touchList);
		touchListView.setAdapter(touchAdapter);
		touchListView.setBackgroundColor(Color.WHITE);
		touchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				
			}
		});
		
		fsrList = new ArrayList<FSRRow>();
		fsrListView = (ListView) findViewById(R.id.fsrList);
		fsrAdapter = new FSRRowAdapter(this, R.layout.fsr_row, fsrList);
		fsrListView.setAdapter(fsrAdapter);
		fsrListView.setBackgroundColor(Color.WHITE);
		fsrListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private void setFSRList(float[] p){
		fsrList.clear();
		for(int i = 0; i < p.length; i++){
			fsrList.add(new FSRRow(p[i]));
		}
		fsrAdapter.notifyDataSetChanged();
	}
	
	private void setTouchList(Point[] t){
		touchList.clear();
		for(int i = 0; i < t.length; i++){
			touchList.add(new TouchRow(t[i].x, t[i].y));
		}
		touchAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void handleFSRMessage(float[] fsr){
		setFSRList(fsr);
	}
	
	@Override
	protected void handleTouchMessage(int[] data){
		int size = (int)data.length/2;
		Point[] t = new Point[size];
		for(int i = 0; i < size; i++){
			int index = i*2;
			t[i] = new Point(data[index], data[index+1]);
		}
		
		setTouchList(t);
	}
	
 }