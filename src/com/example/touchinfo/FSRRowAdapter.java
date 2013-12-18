package com.example.touchinfo;

import java.util.ArrayList;

import com.example.bluetoothclient.R;
import com.example.bluetoothclient.R.id;
import com.example.bluetoothclient.R.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FSRRowAdapter extends ArrayAdapter<FSRRow> {
	private ArrayList<FSRRow> items;
	private LayoutInflater inflater;

	public FSRRowAdapter(Context context, int textViewResourceId, ArrayList<FSRRow> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.fsr_row, null);
		}
		FSRRow item = items.get(position);
		if (item != null) {
			TextView pressure = (TextView) view.findViewById(R.id.pressure);
			if (pressure != null) {
				pressure.setText(String.valueOf(item.getPressure()));
			}
		}
		return view;
	}
}