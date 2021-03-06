
package com.github.andlyticsproject;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.github.andlyticsproject.chart.Chart;

public abstract class BaseChartListAdapter extends BaseAdapter {
	private static String LOG_TAG = BaseChartListAdapter.class.toString();
	/**
	 * 
	 * @return the number of pages that will has the adapter
	 */
	private final int numPages, numColumns[];
	private final int maxColumns;
	private final BaseChartActivity activity;
	private float scale;
	private int currentPage, currentColumn;
	private final boolean usesSmooth;
	private final OnClickListener columnClickListener;

	public abstract int getNumPages();

	/**
	 * 
	 * @param page
	 *          Page to request number of charts (columns) index start at 0
	 * @return Number of charts(columns) that will has the requested page
	 */
	public abstract int getNumCharts(int page) throws IndexOutOfBoundsException;

	/**
	 * 
	 * @param page
	 * @param column
	 * @return Chart title at selected page/column
	 * @throws IndexOutOfBoundsException
	 */
	public abstract String getChartTitle(int page, int column) throws IndexOutOfBoundsException;

	/**
	 * 
	 * @param page
	 * @param column
	 * @return Chart title Drawable at selected page/column or null if none
	 * @throws IndexOutOfBoundsException
	 */
	protected abstract Drawable getChartTitleDrawable(int page, int column);

	/**
	 * 
	 * @param page
	 * @param column
	 * @return Chart subhead line
	 * @throws IndexOutOfBoundsException
	 */
	public abstract String getSubHeadLine(int page, int column) throws IndexOutOfBoundsException;

	/**
	 * 
	 * @param position
	 * @param page
	 * @param column
	 * @return Chart title at selected page/column
	 * @throws IndexOutOfBoundsException
	 */
	public abstract void updateChartValue(int position, int page, int column, TextView tv)
			throws IndexOutOfBoundsException;

	public BaseChartListAdapter(BaseChartActivity activity) {
		this.activity = activity;
		numPages = getNumPages();
		int max = -1;
		boolean useSmoth = false;
		numColumns = new int[numPages];
		for (int i = 0; i < numPages; i++) {
			numColumns[i] = getNumCharts(i);
			max = Math.max(max, numColumns[i]);
			if (useSmothColumn(i))
				useSmoth = true;
		}
		usesSmooth = useSmoth;
		maxColumns = max;
		this.scale = activity.getResources().getDisplayMetrics().density;
		currentPage = 0;
		currentColumn = 1;

		columnClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				int column = (Integer) v.getTag();
				Log.i(LOG_TAG, "Pressed " + column);
				BaseChartListAdapter.this.activity.setCurrentChart(currentPage, column);

			}
		};

	}

	static class ViewHolder {
		final TextView fields[];

		public ViewHolder(int numFields) {
			fields = new TextView[numFields];
		}
	}

	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {

		int i;
		ViewHolder holder;
		if (convertView == null) {
			convertView = activity.getLayoutInflater().inflate(R.layout.base_chart_list_item, null);
			holder = new ViewHolder(maxColumns + (usesSmooth ? 1 : 0));

			for (i = 0; i < maxColumns; i++) {
				holder.fields[i] = createTextView("", false, i > 0);
				if (i > 0) {
					holder.fields[i].setOnClickListener(columnClickListener);
					holder.fields[i].setTag(i);
				}
				((ViewGroup) convertView).addView(holder.fields[i]);
			}
			if (usesSmooth) {
				holder.fields[i] = createTextView("*", false, false);
				((ViewGroup) convertView).addView(holder.fields[i], 1);
			}
			convertView.setTag(holder);

		} else {

			holder = (ViewHolder) convertView.getTag();
		}
		// First field always will be the date
		Typeface typeface = holder.fields[0].getTypeface();
		for (i = 0; i < maxColumns; i++)
			holder.fields[i].setTypeface(typeface, Typeface.NORMAL);
		int diff = maxColumns - numColumns[currentPage];
		for (i = 0; i < maxColumns; i++)
			holder.fields[i].setVisibility(View.VISIBLE);
		if (numColumns[currentPage] < maxColumns) {
			for (i = maxColumns - diff; i < maxColumns; i++)
				holder.fields[i].setVisibility(View.GONE);
		}
		updateChartValue(position, currentPage, 0, holder.fields[0]);
		for (i = 1; i < numColumns[currentPage]; i++)
			updateChartValue(position, currentPage, i, holder.fields[i]);
		holder.fields[currentColumn].setTypeface(typeface, Typeface.BOLD);
		if (usesSmooth) {
			holder.fields[holder.fields.length - 1].setVisibility(isSmothValue(currentPage,
					position) ? View.VISIBLE : View.INVISIBLE);
		}

		return convertView;
	}

	protected abstract boolean useSmothColumn(int page);

	protected abstract boolean isSmothValue(int page, int position);

	private TextView createTextView(String string, boolean bold, boolean weight) {
		TextView view = new TextView(activity);
		view.setText(string);
		int top = (int) (2 * scale);
		int left = (int) (2 * scale);
		view.setPadding(left, top, left, top);
		view.setTextColor(Color.parseColor("#555555"));
		if (weight) {
			view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.WRAP_CONTENT, .2f));
		} else {
			view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));
		}
		view.setGravity(Gravity.RIGHT);
		if (bold) {
			view.setTypeface(view.getTypeface(), Typeface.BOLD);
		}

		return view;
	}

	protected abstract View buildChart(Context context, Chart baseChart, List<?> statsForApp,
			int page, int column) throws IndexOutOfBoundsException;

	public String getCurrentChartTitle() {
		return getChartTitle(currentPage, currentColumn);
	}

	public String getCurrentSubHeadLine() {
		return getSubHeadLine(currentPage, currentColumn);
	}

	public Drawable getCurrentChartTitleDrawable() {
		return getChartTitleDrawable(currentPage, currentColumn);
	}

	public void setCurrentChart(int page, int column) {
		// new Exception().printStackTrace();
		currentPage = page;
		currentColumn = column;
		// Log.i(LOG_TAG,"Seting current "+currentPage+" c="+currentColumn);

	}

	public int getCurrentColumn() {
		// Log.i(LOG_TAG,"Getting  current C "+currentPage+" c="+currentColumn);
		return currentColumn;
	}

	public int getCurrentPage() {
		// Log.i(LOG_TAG,"Getting  current P "+currentPage+" c="+currentColumn);
		return currentPage;
	}

}
