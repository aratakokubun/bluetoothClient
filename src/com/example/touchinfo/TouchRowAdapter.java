package com.example.touchinfo;

import java.util.ArrayList;

import com.example.bluetoothclient.R;
import com.example.bluetoothclient.R.id;
import com.example.bluetoothclient.R.layout;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TouchRowAdapter extends ArrayAdapter<TouchRow> {
	private ArrayList<TouchRow> items;
	private LayoutInflater inflater;

	public TouchRowAdapter(Context context, int textViewResourceId, ArrayList<TouchRow> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.touch_row, null);
		}
		TouchRow item = items.get(position);
		if (item != null) {
			TextView coordinate_x = (TextView) view.findViewById(R.id.coordinate_x);
			TextView coordinate_y = (TextView) view.findViewById(R.id.coordinate_y);
			if (coordinate_x != null) {
				coordinate_x.setText(item.getCoordinateX());
			}
			if (coordinate_y != null) {
				coordinate_y.setText(item.getCoordinateY());
			}
		}
		return view;
	}
}