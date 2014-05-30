package org.owasp.seraphimdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.owasp.seraphimdroid.adapter.PermissionScannerAdapter;
import org.owasp.seraphimdroid.model.PermissionData;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.owasp.seraphimdroid.R;

public class PermissionScannerFragment extends Fragment {

	private static final String TAG = "PermissionScannerFragment";

	private ExpandableListView lvPermissionList;

	private PackageManager pkgManager;

	private boolean isDataChanged;

	// Declaring Containers.
	private ArrayList<String> appList;
	private HashMap<String, List<PermissionData>> childPermissions;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_permission_scanner,
				container, false);

		lvPermissionList = (ExpandableListView) view
				.findViewById(R.id.app_list);

		lvPermissionList.setAdapter(new PermissionScannerAdapter(getActivity(),
				appList, childPermissions));

		lvPermissionList.setClickable(true);

		lvPermissionList.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView list,
					View clickedView, int groupPos, int childPos, long childId) {

				Log.d(TAG, "Starting Permission Description");
				
				PermissionData perData = childPermissions.get(
						appList.get(groupPos)).get(childPos);

				String permission = perData.getPermission();

				Intent startPerDesc = new Intent(getActivity(),
						PermissionDescription.class);

				startPerDesc.putExtra("PERMISSION_NAME", permission);
				startActivity(startPerDesc);

				return true;
			}
		});

		isDataChanged = true;

		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		pkgManager = getActivity().getPackageManager();
		isDataChanged = true;

		// Initializing Containers.
		appList = new ArrayList<String>();
		childPermissions = new HashMap<String, List<PermissionData>>();
		// appList.clear();
		// childPermissions.clear();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();

		if (isDataChanged) {
			prepareList();
		}

		Log.d(TAG, "Ending onResume");
	}

	private void prepareList() {
		// Clear previous data.
		appList.clear();
		childPermissions.clear();

		new AsyncListGenerator().execute();

		Log.d(TAG, "Preparing list");

	}

	private boolean isSystemPackage(PackageInfo packageInfo) {
		return ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true
				: false;
	}

	private class AsyncListGenerator extends AsyncTask<Void, Void, Void> {

		ProgressDialog loading = new ProgressDialog(getActivity());

		@Override
		protected void onPreExecute() {
			loading.setTitle("Scanning Permissions");
			loading.setMessage("Please Wait...");
			loading.show();
			loading.setCancelable(false);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			List<ApplicationInfo> installedApps = pkgManager
					.getInstalledApplications(PackageManager.GET_META_DATA);

			PermissionGetter permissionGetter = new PermissionGetter(pkgManager);

			for (ApplicationInfo appInfo : installedApps) {

				PackageInfo pkgInfo;
				try {
					pkgInfo = pkgManager.getPackageInfo(appInfo.packageName,
							PackageManager.GET_PERMISSIONS);

					List<PermissionData> reqPermissions = new ArrayList<PermissionData>();

					if (!isSystemPackage(pkgInfo)) {

						appList.add(pkgInfo.packageName);

						String[] appPermissions = pkgInfo.requestedPermissions;

						// Log.d(TAG, "Adding " + appPermissions[0]);

						if (appPermissions != null) {

							for (String permission : appPermissions) {
								if (permissionGetter
										.generatePermissionData(permission) != null) {
									reqPermissions
											.add(permissionGetter
													.generatePermissionData(permission));

									Log.d(TAG,
											"Adding permissions to reqPermissions");

								}
							}

						} else {
							reqPermissions.add(new PermissionData(
									"No Permission", "No Permission",
									"No Permission", "No Permission", 0));
						}

						childPermissions.put(pkgInfo.packageName,
								reqPermissions);
						Log.d(TAG, "Adding childItem");

					}

				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			Log.d(TAG, "returning");
			isDataChanged = false;
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// Setting Adapters for the list view
			lvPermissionList.setAdapter(new PermissionScannerAdapter(
					getActivity(), appList, childPermissions));
			loading.dismiss();
			super.onPostExecute(result);
		}

	}

}
