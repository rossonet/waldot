package net.rossonet.zenoh.exception;

import org.json.JSONArray;
import org.json.JSONObject;

public class ExecutionCommandException extends WaldotZenohException {

	private static final String CLASS_NAME = "className";
	private static final String FILE_NAME = "fileName";
	private static final String LINE_NUMBER = "lineNumber";
	private static final String MESSAGE_FIELD = "message";
	private static final String METHOD_NAME = "methodName";
	private static final long serialVersionUID = -2347588582772682544L;
	private static final String STACK_FIELD = "stack";

	public static ExecutionCommandException fromJson(JSONObject exception) {
		final ExecutionCommandException result = new ExecutionCommandException(exception.getString(MESSAGE_FIELD),
				true);
		final JSONArray stackArray = exception.getJSONArray(STACK_FIELD);

		final StackTraceElement[] stackTrace = new StackTraceElement[stackArray.length()];
		for (int i = 0; i < stackArray.length(); i++) {
			final JSONObject element = stackArray.getJSONObject(i);
			final StackTraceElement stackTraceElement = new StackTraceElement(element.getString(CLASS_NAME),
					element.getString(METHOD_NAME), element.optString(FILE_NAME, null), element.getInt(LINE_NUMBER));
			stackTrace[i] = stackTraceElement;
		}
		result.setStackTrace(stackTrace);
		return result;
	}

	public ExecutionCommandException(String message) {
		super(message, null);
	}

	public ExecutionCommandException(String message, boolean writableStackTrace) {
		super(message, null, true, writableStackTrace);
	}

	public ExecutionCommandException(String message, Throwable exception) {
		super(message, exception);
	}

	public JSONObject toJson() {
		final StackTraceElement[] stackTrace = getStackTrace();
		final JSONArray stack = new JSONArray();
		for (final StackTraceElement stackTraceElement : stackTrace) {
			final JSONObject element = new JSONObject();
			element.put(CLASS_NAME, stackTraceElement.getClassName());
			element.put(METHOD_NAME, stackTraceElement.getMethodName());
			element.put(FILE_NAME, stackTraceElement.getFileName());
			element.put(LINE_NUMBER, stackTraceElement.getLineNumber());
			stack.put(element);
		}
		final JSONObject result = new JSONObject();
		result.put(MESSAGE_FIELD, getMessage());
		result.put(STACK_FIELD, stack);
		return result;
	}

}
