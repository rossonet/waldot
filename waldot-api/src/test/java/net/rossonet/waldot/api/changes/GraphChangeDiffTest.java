package net.rossonet.waldot.api.changes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.changes.GraphChangeRecorder.Change;

class GraphChangeDiffTest {

	private static Change async(final String kind, final String element) {
		return new Change(kind, element, kind.startsWith("edge") ? "edge" : "vertex", null, null, null, null,
				GraphChangeRecorder.ORIGIN_ASYNC, "other", null);
	}

	private static Change change(final String kind, final String element, final Map<String, Object> snapshot) {
		return new Change(kind, element, kind.startsWith("edge") ? "edge" : "vertex", null, null, null, snapshot,
				GraphChangeRecorder.ORIGIN_SCRIPT, "main", null);
	}

	private static Change property(final String element, final String key, final Object oldValue,
			final Object newValue) {
		return new Change("property", element, "vertex", key, oldValue, newValue, null,
				GraphChangeRecorder.ORIGIN_SCRIPT, "main", null);
	}

	@SuppressWarnings("unchecked")
	private static <T> T at(final Map<String, Object> map, final String... path) {
		Object current = map;
		for (final String p : path) {
			current = ((Map<String, Object>) current).get(p);
		}
		return (T) current;
	}

	@Test
	void asyncChangesAreOnlyCounted() {
		final Map<String, Object> diff = GraphChangeDiff.diff(
				List.of(async("vertex-added", "x"), async("vertex-added", "y"), async("property", "z")), false);
		assertEquals(0, (int) at(diff, "summary", "vertices_created"));
		assertEquals(2, (int) at(diff, "async", "vertex-added"));
		assertEquals(1, (int) at(diff, "async", "property"));
	}

	@Test
	void edgesRemovedWithTheirVertexAreNotListedAsCreated() {
		final Map<String, Object> snapshot = Map.of("id", "v1", "properties", Map.of(), "edges",
				List.of(Map.of("id", "e1")));
		final Map<String, Object> diff = GraphChangeDiff.diff(List.of(change("vertex-added", "v1", null),
				change("edge-added", "e1", null), change("vertex-removed", "v1", snapshot)), false);
		assertEquals(0, (int) at(diff, "summary", "vertices_created"));
		assertEquals(0, (int) at(diff, "summary", "edges_created"));
		assertEquals(0, (int) at(diff, "summary", "vertices_removed"));
	}

	@Test
	void emptyDiff() {
		final Map<String, Object> diff = GraphChangeDiff.diff(List.of(), false);
		assertEquals(0, (int) at(diff, "summary", "properties_modified"));
		assertTrue(((Map<?, ?>) diff.get("by_type")).isEmpty());
		assertFalse((boolean) diff.get("truncated"));
	}

	@Test
	void propertyChangedBackToTheOldValueIsNotModified() {
		final Map<String, Object> diff = GraphChangeDiff.diff(
				List.of(property("v", "x", 1, 2), property("v", "x", 2, 1)), false);
		assertEquals(0, (int) at(diff, "summary", "properties_modified"));
	}

	@Test
	void propertyKeepsFirstOldAndLastNew() {
		final Map<String, Object> diff = GraphChangeDiff.diff(
				List.of(property("v", "x", 1, 2), property("v", "x", 2, 3), property("v", "y", null, "a")), false);
		final List<Map<String, Object>> props = at(diff, "properties");
		assertEquals(2, props.size());
		assertEquals(1, props.get(0).get("old"));
		assertEquals(3, props.get(0).get("new"));
		assertEquals("vertex", props.get(0).get("target"));
		assertEquals(null, props.get(1).get("old"));
	}

	@Test
	void propertiesOfRemovedVerticesAreNotModified() {
		final Map<String, Object> diff = GraphChangeDiff.diff(List.of(property("v", "x", 1, 2),
				change("vertex-removed", "v", Map.of("id", "v", "properties", Map.of("x", 2)))), false);
		assertEquals(0, (int) at(diff, "summary", "properties_modified"));
		assertEquals(1, (int) at(diff, "summary", "vertices_removed"));
	}

	@Test
	void removedVerticesAreGroupedByTheirType() {
		final Map<String, Object> diff = GraphChangeDiff.diff(List.of(
				change("vertex-removed", "a", Map.of("id", "a", "properties", Map.of("type", "rules:rule"))),
				change("vertex-removed", "b", Map.of("id", "b", "properties", Map.of("type", "rules:rule"))),
				change("vertex-removed", "c", Map.of("id", "c", "properties", Map.of()))), false);
		assertEquals(2, (int) at(diff, "by_type", "rules:rule", "removed"));
		assertEquals(1, (int) at(diff, "by_type", "vertex", "removed"));
	}

	@Test
	void runtimePropertiesAreOnlyCounted() {
		final Map<String, Object> diff = GraphChangeDiff.diff(
				List.of(property("v", "run!status", "a", "b"), property("v", "run!errors", 0, 1)), false);
		assertEquals(0, (int) at(diff, "summary", "properties_modified"));
		assertEquals(2, (int) at(diff, "summary", "runtime_changes"));
	}

	@Test
	void truncatedIsReported() {
		assertTrue((boolean) GraphChangeDiff.diff(List.of(), true).get("truncated"));
	}

	@Test
	void recorderLimitTruncates() {
		final List<Change> seen = new ArrayList<>();
		try (GraphChangeRecorder.Recording r = GraphChangeRecorder.start(2)) {
			GraphChangeRecorder.onPropertyChanging(null, "k", 1, 2);
			assertFalse(r.isTruncated(), "null elements are ignored");
			assertEquals(1, GraphChangeRecorder.activeRecordings());
			seen.addAll(r.changes());
		}
		assertEquals(0, GraphChangeRecorder.activeRecordings());
		assertTrue(seen.isEmpty());
		assertFalse(GraphChangeRecorder.isRecording());
	}

	@Test
	void plainValues() {
		assertEquals(1, GraphChangeRecorder.plain(1));
		assertEquals("x", GraphChangeRecorder.plain("x"));
		assertEquals(true, GraphChangeRecorder.plain(true));
		assertEquals("[1, 2]", GraphChangeRecorder.plain(List.of(1, 2)));
		assertEquals(null, GraphChangeRecorder.plain(null));
	}
}
