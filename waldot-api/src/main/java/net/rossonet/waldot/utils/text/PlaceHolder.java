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

	/**
	 * Creates a PlaceHolder that removes placeholder tags from the data.
	 * 
	 * @param data                    the original data containing placeholder tags
	 * @param startPlaceholderText    the start tag to remove
	 * @param stopPlaceholderText    the stop tag to remove
	 */
	public PlaceHolder(final String data, final String startPlaceholderText, final String stopPlaceholderText) {
		originalData = data;
		this.data = originalData.replaceAll("^" + startPlaceholderText, "").replaceAll(stopPlaceholderText + "$", "");
	}

	/**
	 * Returns the data with placeholder tags removed.
	 * 
	 * @return the data without start and stop placeholder tags
	 */
	public String getDataWithoutPlaceholderTag() {
		return data;
	}
}
