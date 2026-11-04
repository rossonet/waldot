package net.rossonet.waldot.jexl;

import org.apache.commons.jexl3.JexlException;

import net.rossonet.waldot.jexl.JexlErrorFormatter.JexlErrorInfo;

/**
 * Error of a JEXL script with a readable message (line, column, cause and
 * excerpt, see {@link JexlErrorFormatter}). The original
 * {@link JexlException} is the cause.
 */
public class WaldotJexlException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient JexlErrorInfo info;

	/**
	 * @param source the script source
	 * @param cause  the exception raised by JEXL
	 */
	public WaldotJexlException(final String source, final JexlException cause) {
		this(JexlErrorFormatter.describe(source, cause), cause);
	}

	private WaldotJexlException(final JexlErrorInfo info, final JexlException cause) {
		super(info.toMessage(), cause);
		this.info = info;
	}

	/** Structured description of the error. */
	public JexlErrorInfo getInfo() {
		return info;
	}

	/** The exception raised by JEXL. */
	public JexlException getJexlException() {
		return (JexlException) getCause();
	}
}
