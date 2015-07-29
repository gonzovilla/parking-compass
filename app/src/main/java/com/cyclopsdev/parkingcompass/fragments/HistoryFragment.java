package com.cyclopsdev.parkingcompass.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cyclopsdev.parkingcompass.R;
import com.cyclopsdev.parkingcompass.activities.MapHistoryActivity;
import com.cyclopsdev.parkingcompass.support.HistoryAdapter;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends ListFragment {

	private List<ParseObject> mLocationsList;
	private ListView mListView;
	private HistoryAdapter mAdapter;
	private ProgressBar mProgressBar;

	private static final String SECTION_NUMBER = "Section";
	private Context mContext;

	public HistoryFragment() {
	}

	public static HistoryFragment newInstance(int section) {
		HistoryFragment fragment = new HistoryFragment();
		Bundle args = new Bundle();
		args.putInt(SECTION_NUMBER, section);
		fragment.setArguments(args);
		return fragment;
	}

	public void historyModified (){

		getParseAndSetAdapter();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		return inflater.inflate(R.layout.history_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view,savedInstanceState);
		mListView = getListView();
		mContext = getActivity();

		TextView emptyText = (TextView)getActivity().findViewById(R.id.text_view_empty_list);
		mListView.setEmptyView(emptyText);
		mProgressBar = (ProgressBar)getActivity().findViewById(R.id.list_progress_bar);

		getParseAndSetAdapter();

	}

	private void getParseAndSetAdapter() {

		mProgressBar.setVisibility(View.VISIBLE);

		ParseQuery<ParseObject> query = ParseQuery.getQuery("Location");
		query.whereEqualTo("user", Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));
		query.orderByDescending("createdAt");
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> locationsList, ParseException e) {
				if (e == null) {
					if (mLocationsList == null) {
						mLocationsList = locationsList;
					} else {
						mLocationsList.clear();
						mLocationsList.addAll(locationsList);
					}

					if (mAdapter == null) {
						mAdapter = new HistoryAdapter(mContext, mLocationsList);
						setListAdapter(mAdapter);
						setLongClickToRemove();
					} else {
						mListView.invalidate();
						mAdapter.notifyDataSetChanged();
					}

					mProgressBar.setVisibility(View.GONE);

				} else {
					Log.d("LOCATION", "Error: " + e.getMessage());
				}
			}
		});

	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		ParseObject location = mLocationsList.get(position);

		Intent mapIntent= new Intent(getActivity(), MapHistoryActivity.class);

		mapIntent.putExtra("latitude", location.getDouble("latitude"));
		mapIntent.putExtra("longitude", location.getDouble("longitude"));
		mapIntent.putExtra("address", location.getString("address"));

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMMM, yyyy HH:mm", Locale.getDefault());
		String date = simpleDateFormat.format(location.getCreatedAt());
		mapIntent.putExtra("date", date);

		getActivity().startActivity(mapIntent);

	}

	private void setLongClickToRemove() {
		mListView.setLongClickable(true);
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
				dialogBuilder.setTitle(getString(R.string.remove_history_title));
				dialogBuilder.setMessage(R.string.remove_history_message);
				dialogBuilder.setPositiveButton(getString(R.string.remove), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						ParseObject parseLocation = mLocationsList.get(position);
						parseLocation.deleteInBackground();

						mLocationsList.remove(position);
						mAdapter.notifyDataSetChanged();

						Toast.makeText(getActivity(), getString(R.string.item_removed), Toast.LENGTH_SHORT).show();
					}
				});
				dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				dialogBuilder.create().show();
				return true;

			}
		});
	}

}