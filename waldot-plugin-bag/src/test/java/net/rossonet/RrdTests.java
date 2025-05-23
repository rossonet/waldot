package net.rossonet;

import static org.rrd4j.ConsolFun.AVERAGE;

import java.awt.Color;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

public class RrdTests {

	public static String rrdPath = "my.rrd";
	private static final int STEPS = 12 * 60;
	private static final int TAG_NUMBER = 10000;
	private final static Map<Integer, String> tags = new HashMap<>();

	@BeforeAll
	public static void setupAll() {
		for (int tagNum = 0; tagNum < TAG_NUMBER; tagNum++) {
			tags.put(tagNum, "tag_" + tagNum);
		}
	}

	private long lastSampleTime;
	private final Random rand = new Random();
	private RrdDef rrdDef;
	private Instant start;

	private double generateNewValue(int type, double lastValue) {
		switch (type) {
		case 0:
			return rand.nextDouble() * 999;
		case 1:
			return lastValue + 10;
		case 2:
			return lastValue + rand.nextDouble() * 10;
		default:
			return 0;
		}
	}

	@BeforeEach
	public void setup() {
		rrdDef = new RrdDef(rrdPath);
		rrdDef.setStep(5);
		for (final String tag : tags.values()) {
			rrdDef.addDatasource(tag, DsType.GAUGE, 60000, 0, 1000);
		}
		rrdDef.addArchive(AVERAGE, 0.5, 1, 10000);
		// rrdDef.addArchive(LAST, 0.5, 1, 700);
		// rrdDef.addArchive(MAX, 0.5, 1, 700);
		// rrdDef.addArchive(MIN, 0.5, 1, 700);
	}

	@Test
	public void testCheckData() throws IOException {
		testLoad();
		final RrdDb rrdDb = RrdDb.of(rrdPath);
		rrdDb.exportXml("test_export.xml");
		System.out.println(rrdDb.dump());
		rrdDb.close();
	}

	@Test
	public void testGraph() throws IOException {
		testLoad();
		final RrdGraphDef gDef = new RrdGraphDef(start.getEpochSecond(), lastSampleTime);
		gDef.setWidth(800);
		gDef.setHeight(400);
		gDef.setFilename("example_rrd.png");
		gDef.setTitle("Grafico di esempio");
		gDef.setVerticalLabel("valore");
		int c = 1;
		final Color[] colors = new Color[] { Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN,
				Color.MAGENTA, Color.ORANGE, Color.PINK, Color.LIGHT_GRAY, Color.DARK_GRAY };
		final int limit = 1;
		for (final String tag : tags.values()) {
			gDef.datasource(tag + "_avg", rrdPath, tag, AVERAGE);
			// gDef.datasource(tag + "_last", rrdPath, tag, LAST);
			final Color color = colors[c % limit];
			gDef.line(tag + "_avg", color, tag);
			// gDef.line(tag + "_last", color, "last " + tag);
			c++;
			if (c > limit) {
				break;
			}
		}
		gDef.setImageFormat("png");
		new RrdGraph(gDef); // will create the graph in the path specified
		System.out.println("Graph created");
	}

	@Test
	public void testLoad() throws IOException {
		final RrdDb rrdDb = RrdDb.getBuilder().setRrdDef(rrdDef).build();
		start = Instant.now();
		double lastValue = 0;
		lastSampleTime = start.getEpochSecond();
		for (int i = 1; i < STEPS; i++) {
			final Sample sample = rrdDb.createSample(lastSampleTime);
			lastSampleTime += 5;
			for (final Integer tag : tags.keySet()) {
				final double newValue = generateNewValue(tag % 3, lastValue);
				sample.setValue(tag, newValue);
				lastValue = newValue;

			}
			sample.update();
		}
		final Instant stop = Instant.now();
		System.out.println("Data from " + start.toString() + " to " + Instant.ofEpochSecond(lastSampleTime).toString());
		System.out.println("Time (secs): " + (stop.getEpochSecond() - start.getEpochSecond()));
		rrdDb.close();
	}
}
