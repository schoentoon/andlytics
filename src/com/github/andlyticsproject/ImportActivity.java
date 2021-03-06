package com.github.andlyticsproject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.github.andlyticsproject.io.ImportService;
import com.github.andlyticsproject.io.ServiceExceptoin;
import com.github.andlyticsproject.io.StatsCsvReaderWriter;
import com.github.andlyticsproject.util.DetachableAsyncTask;
import com.github.andlyticsproject.util.Utils;

public class ImportActivity extends SherlockActivity {

	private static final String TAG = ImportActivity.class.getSimpleName();

	public static final int TAG_IMAGE_REF = R.id.tag_mainlist_image_reference;

	private ImportListAdapter adapter;

	private LayoutInflater layoutInflater;

	private List<String> importFileNames = new ArrayList<String>();

	private String accountName;

	private ContentAdapter db;

	private ListView listView;

	private LoadImportDialogTask loadTask;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.import_stats);

		layoutInflater = getLayoutInflater();
		db = ((AndlyticsApp) getApplication()).getDbAdapter();

		accountName = getAccountName();
		getSupportActionBar().setSubtitle(accountName);

		View closeButton = (View) this.findViewById(R.id.import_dialog_close_button);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		View importButton = (View) this.findViewById(R.id.import_dialog_import_button);
		importButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (importFileNames.size() == 0) {
					Toast.makeText(ImportActivity.this, getString(R.string.import_no_app),
							Toast.LENGTH_LONG).show();
				} else {
					Intent intent = new Intent(ImportActivity.this, ImportService.class);
					intent.setData(getIntent().getData());
					intent.putExtra(ImportService.FILE_NAMES,
							importFileNames.toArray(new String[importFileNames.size()]));
					intent.putExtra(ImportService.ACCOUNT_NAME, accountName);
					startService(intent);
					finish();
				}
			}
		});

		listView = (ListView) this.findViewById(R.id.list_view_id);
		listView.addHeaderView(layoutInflater.inflate(R.layout.import_list_header, null), null,
				false);
		setFilenames(new ArrayList<String>());

		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			Uri data = getIntent().getData();
			if (data == null) {
				Toast.makeText(this, "Stats file not specified as data.", Toast.LENGTH_LONG).show();
				finish();
			}


			if (getLastNonConfigurationInstance() != null) {
				loadTask = (LoadImportDialogTask) getLastNonConfigurationInstance();
				loadTask.attach(this);
				setFilenames(loadTask.getFilenames());
			} else {
				loadTask = new LoadImportDialogTask(this);
				Utils.execute(loadTask, data.getPath());
			}
		} else {
			Log.w(TAG, "Don't know how to handle this action: " + getIntent().getAction());
			finish();
		}
	}

	List<String> getPackagesForAccount() {
		return db.getPackagesForAccount(accountName);
	}

	void setFilenames(List<String> filenames) {
		adapter = new ImportListAdapter(filenames);
		listView.setAdapter(adapter);
	}

	private String getAccountName() {
		String ownerAccount = StatsCsvReaderWriter.getAccountNameForExport(new File(getIntent()
				.getData().getPath()).getName());
		if (ownerAccount == null) {
			// fall back to value from preferences
			// XXX should we give a choice instead?
			ownerAccount = Preferences.getAccountName(this);
		}
		return ownerAccount;
	}

	public Object onRetainNonConfigurationInstance() {
		return loadTask == null ? null : loadTask.detach();
	}

	private static class LoadImportDialogTask extends
			DetachableAsyncTask<String, Void, Boolean, ImportActivity> {

		LoadImportDialogTask(ImportActivity parent) {
			super(parent);
		}

		private List<String> filenames = new ArrayList<String>();
		private String zipFilename;

		@Override
		protected void onPreExecute() {
			activity.setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected Boolean doInBackground(String... params) {
			if (activity == null) {
				return false;
			}

			zipFilename = params[0];
			List<String> pacakgeNames = activity.getPackagesForAccount();
			try {
				filenames = StatsCsvReaderWriter.getImportFileNamesFromZip(
						activity.getAccountName(), pacakgeNames, zipFilename);

				return true;
			} catch (ServiceExceptoin e) {
				Log.e(TAG, "Error reading import zip file: " + e.getMessage());
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (activity == null) {
				return;
			}

			activity.setProgressBarIndeterminateVisibility(false);

			if (!activity.isFinishing()) {
				if (result) {
					activity.setFilenames(filenames);
				} else {
					Toast.makeText(activity,
							"SD-Card not mounted or invalid file format, can't import!",
							Toast.LENGTH_LONG).show();
					activity.finish();
				}
			}
		}

		List<String> getFilenames() {
			return filenames;
		}

	}

	class ImportListAdapter extends BaseAdapter {

		List<String> files;

		ImportListAdapter(List<String> files) {
			this.files = files;
		}

		@Override
		public int getCount() {
			return files.size();
		}

		@Override
		public String getItem(int position) {
			return files.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;

			if (convertView == null) {

				convertView = layoutInflater.inflate(R.layout.import_list_item, null);

				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.import_file_name);
				holder.row = (RelativeLayout) convertView.findViewById(R.id.import_app_row);
				holder.checkbox = (CheckBox) convertView.findViewById(R.id.import_file_checkbox);
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			final String fileName = getItem(position);
			holder.name.setText(StatsCsvReaderWriter.getPackageName(fileName));

			holder.checkbox.setChecked(importFileNames.contains(fileName));

			holder.row.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					CheckBox checkbox = ((CheckBox) (((ViewGroup) v)
							.findViewById(R.id.import_file_checkbox)));
					checkbox.setChecked(!checkbox.isChecked());

					if (checkbox.isChecked()) {
						importFileNames.add(fileName);
					} else {
						importFileNames.remove(fileName);
					}
				}
			});

			holder.checkbox.setTag(fileName);

			holder.checkbox.setOnClickListener(new CheckBox.OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean isChecked = ((CheckBox) v).isChecked();
					if (isChecked) {
						importFileNames.add(fileName);
					} else {
						importFileNames.remove(fileName);
					}

				}
			});

			return convertView;
		}

		private class ViewHolder {
			public RelativeLayout row;
			public TextView name;
			public CheckBox checkbox;
		}
	}

}
