package de.handler.mobile.android.videobox;

final class MessageHelper {
	static final int SHOW_CAMERA = 0;
	static final int SHOW_REMOTE = 1;
	static final int CONNECTED = 2;
	static final int TOGGLE_CAMERA = 3;

	private MessageHelper() {
		// prevent instantiation
	}


	static byte[] mapPayload(int toConvert) {
		byte[] ret = new byte[4];
		ret[3] = (byte) (toConvert & 0xFF);
		ret[2] = (byte) ((toConvert >> 8) & 0xFF);
		ret[1] = (byte) ((toConvert >> 16) & 0xFF);
		ret[0] = (byte) ((toConvert >> 24) & 0xFF);
		return ret;
	}

	static int unmapPayload(byte[] payload) {
		return (payload[3] & 0xFF) +
				((payload[2] & 0xFF) << 8) +
				((payload[1] & 0xFF) << 16) +
				((payload[0] & 0xFF) << 24);
	}
}
