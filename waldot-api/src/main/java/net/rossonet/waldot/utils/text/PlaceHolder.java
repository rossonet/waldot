package net.rossonet.waldot.utils.text;

/**
 * PlaceHolder class is used to handle text data that may contain placeholder
 * tags. It allows for the extraction of the data without the placeholder tags.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class PlaceHolder {

	private final String data;
	private final String originalData;

	public PlaceHolder(final String data, final String startPlaceholderText, final String stopPlaceholderText) {
		originalData = data;
		this.data = originalData.replaceAll("^" + startPlaceholderText, "").replaceAll(stopPlaceholderText + "$", "");
	}

	public String getDataWithoutPlaceholderTag() {
		return data;
	}
}
