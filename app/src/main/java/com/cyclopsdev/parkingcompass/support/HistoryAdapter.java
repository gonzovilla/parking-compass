package com.cyclopsdev.parkingcompass.support;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cyclopsdev.parkingcompass.R;
import com.parse.ParseObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by gonzovilla89 on 06/04/15.
 */
public class HistoryAdapter extends ArrayAdapter<ParseObject> {

    private List<ParseObject> mLocationsList;
    private LayoutInflater mLayoutInflater;

    public HistoryAdapter (Context context, List<ParseObject> locationsList){
        super(context, R.layout.history_item, locationsList);

        if (mLocationsList == null ) {
            mLocationsList = locationsList;
        } else {
            mLocationsList.clear();
            mLocationsList.addAll(locationsList);
        }

        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    static class ViewHolder{
        TextView mTextView1;
        TextView mTextView2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        ParseObject location = mLocationsList.get(position);

        if(convertView==null) {
            convertView = mLayoutInflater.inflate(R.layout.history_item, parent, false);
            holder = new ViewHolder();

            holder.mTextView1 = (TextView) convertView.findViewById(R.id.text1);
            holder.mTextView2 = (TextView) convertView.findViewById(R.id.text2);

            convertView.setTag(holder);

        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        if (!(position % 2 == 0)){
            convertView.setBackgroundColor(Color.LTGRAY);
        } else {
            convertView.setBackgroundColor(Color.WHITE);
        }

        String text1 = location.getString("address");
        if (text1 == null || text1.equals("")) {
            text1 = "Lat.: " + Double.toString(location.getDouble("latitude")) + "\nLon.: " + Double.toString(location.getDouble("longitude"));
        }

        Date creationDate = location.getCreatedAt();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM, yyyy HH:mm", Locale.getDefault());
        String text2 = simpleDateFormat.format(creationDate);

        holder.mTextView1.setText(text1);
        holder.mTextView2.setText(text2);

        return convertView;
    }
}
