package de.handler.mobile.android.videobox;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

// TODO Extract connection logic to a manager class and implement interface here to be able to also use other technologies
public abstract class AbstractNearbyActivity extends AbstractActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final int TIMEOUT_PUBLISH = 3 * 60;
	private static final Strategy PUB_SUB_STRATEGY =
			new Strategy.Builder().setTtlSeconds(TIMEOUT_PUBLISH).build();
	private final MessageListener mMessageListener = new MessageListener() {
		@Override
		public void onFound(final Message message) {
			handleMessage(MessageHelper.unmapPayload(message.getContent()));
		}

		@Override
		public void onLost(final Message message) {
			// Called when a message is no longer detectable nearby.
			// Currently not important;
		}
	};
	protected GoogleApiClient mGoogleApiClient;
	protected Message mMessage;

	protected abstract void onNearbyConnected();

	protected abstract void showCamera();

	protected abstract void showRemote();

	protected abstract void toggleCamera();


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.initNearbyServices();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			unpublish();
			unsubscribe();
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		this.onNearbyConnected();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		if (BuildConfig.DEBUG) {
			Log.e(AbstractNearbyActivity.class.getName(), "GoogleApiClient disconnected with cause: " + cause);
		}
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		if (result.hasResolution()) {
			try {
				result.startResolutionForResult(AbstractNearbyActivity.this, REQUEST_RESOLVE_ERROR);
			} catch (IntentSender.SendIntentException e) {
				if (BuildConfig.DEBUG) {
					Log.e(AbstractNearbyActivity.class.getName(),
							"GoogleApiClient caused an SendIntentException with message: " + e.getMessage());
				}
			}
		} else {
			if (BuildConfig.DEBUG) {
				Log.e(AbstractNearbyActivity.class.getName(), "GoogleApiClient connection failed");
			}
			showInfo(R.string.error_nearby_connection_failed);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			if (resultCode == RESULT_OK) {
				mGoogleApiClient.connect();
			} else if (BuildConfig.DEBUG) {
				Log.e(AbstractNearbyActivity.class.getName(),
						"GoogleApiClient connection failed. Unable to resolve.");
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}



	protected void subscribe() {
		SubscribeOptions options = new SubscribeOptions.Builder()
				.setStrategy(PUB_SUB_STRATEGY)
				.setCallback(new SubscribeCallback() {
					@Override
					public void onExpired() {
						super.onExpired();
					}
				}).build();

		Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
				.setResultCallback(status -> {
					if (!status.isSuccess()) {
						showInfo(getString(R.string.error_nearby_publish,
								NearbyMessagesStatusCodes.getStatusCodeString(status.getStatusCode())));
					}
				});
	}

	protected void publish(int message) {
		PublishOptions options = new PublishOptions.Builder()
				.setStrategy(PUB_SUB_STRATEGY)
				.setCallback(new PublishCallback() {
					@Override
					public void onExpired() {
						super.onExpired();
					}
				}).build();

		byte[] toByte = MessageHelper.mapPayload(message);
		mMessage = new Message(toByte);

		Nearby.Messages.publish(mGoogleApiClient, mMessage, options)
				.setResultCallback(status -> {
					if (!status.isSuccess()) {
						showInfo(getString(R.string.error_nearby_subscribe,
								NearbyMessagesStatusCodes.getStatusCodeString(status.getStatusCode())));
					}
				});
	}

	protected void unsubscribe() {
		Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
	}

	protected void unpublish() {
		if (mMessage != null) {
			Nearby.Messages.unpublish(mGoogleApiClient, mMessage);
		}
	}

	private void initNearbyServices() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Nearby.MESSAGES_API)
				.addConnectionCallbacks(this)
				.enableAutoManage(this, this)
				.build();
	}

	private void handleMessage(int message) {
		switch (message) {
			case MessageHelper.CONNECTED:
				showInfo("Devices successfully paired");
				this.publish(MessageHelper.SHOW_REMOTE);
				break;
			case MessageHelper.SHOW_REMOTE:
				showRemote();
				this.publish(MessageHelper.SHOW_CAMERA);
				break;
			case MessageHelper.SHOW_CAMERA:
				showCamera();
				break;
			case MessageHelper.TOGGLE_CAMERA:
				toggleCamera();
				break;
			default:
		}
	}
}
