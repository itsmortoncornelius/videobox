package de.handler.mobile.android.videobox;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

public class ConnectivityHelper {
	private ConnectivityHelper() {
		// prevent initialization
	}

	public static boolean isConnectedToNetwork(@NonNull Context context) {
		ConnectivityManager connManager =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Network[] networks = connManager.getAllNetworks();
		for (Network networkType : networks) {
			NetworkInfo info = connManager.getNetworkInfo(networkType);
			if (info != null && info.isConnectedOrConnecting()) {
				return true;
			}
		}
		return false;
	}
}
