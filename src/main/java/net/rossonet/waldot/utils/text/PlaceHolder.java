package net.rossonet.waldot.utils.text;

public class PlaceHolder {

	private final String data;
	private final String originalData;

	public PlaceHolder(String data, String startPlaceholderText, String stopPlaceholderText) {
		originalData = data;
		this.data = originalData.replaceAll("^" + startPlaceholderText, "").replaceAll(stopPlaceholderText + "$", "");
	}

	public String getDataWithoutPlaceholderTag() {
		return data;
	}
}
