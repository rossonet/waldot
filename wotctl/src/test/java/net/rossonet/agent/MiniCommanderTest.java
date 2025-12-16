
package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

public class MiniCommanderTest {

	private Table<String> createTable() {
		final Table<String> t = new Table<>("Name", "Size", "Modified");
		t.setSelectAction(() -> {
			// placeholder: si potrebbero aprire directory o visualizzare file
		});
		return t;
	}

	/* --------- IMPLEMENTAZIONE ---------- */

	private void loadDir(Table<String> table, Path dir) throws IOException {
		table.getTableModel().clear();
		final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
			for (final Path p : ds) {
				final String name = p.getFileName().toString();
				final String size = Files.isDirectory(p) ? "<DIR>" : String.valueOf(Files.size(p));
				final String mod = fmt.format(Files.getLastModifiedTime(p).toInstant().atZone(ZoneId.systemDefault()));
				table.getTableModel().addRow(name, size, mod);
			}
		}
	}

	private void runMiniCommander(Path initialPath) throws IOException, InterruptedException {
		final DefaultTerminalFactory factory = new DefaultTerminalFactory().setForceTextTerminal(true);
		final Screen screen = factory.createScreen();
		screen.startScreen();

		final MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
		final BasicWindow window = new BasicWindow("Mini Commander");

		// layout principale: due tabelle affiancate
		final Panel root = new Panel(new LinearLayout(Direction.HORIZONTAL));
		final Table<String> left = createTable(); // colonna sinistra
		final Table<String> right = createTable(); // colonna destra
		root.addComponent(left.withBorder(Borders.singleLine("Left")));
		root.addComponent(right.withBorder(Borders.singleLine("Right")));

		// barra inferiore tasti funzione
		final Label footer = new Label("F3 View  F5 Copy  F10 Quit").addStyle(SGR.BOLD)
				.setBackgroundColor(TextColor.ANSI.BLUE).setForegroundColor(TextColor.ANSI.WHITE);

		final Panel container = new Panel(new BorderLayout());
		container.addComponent(root, BorderLayout.Location.CENTER);
		container.addComponent(footer, BorderLayout.Location.BOTTOM);
		window.setComponent(container);

		// carica contenuto directory iniziale
		loadDir(left, initialPath);
		loadDir(right, initialPath);

		// listener tastiera
		window.addWindowListener(new WindowListenerAdapter() {
			@Override
			public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean in) {
				if (key.getKeyType() == KeyType.F10) {
					window.close();
					in.set(true);
				}
			}

		});

		// chiudi dopo 60 s se l’utente non ha premuto F10
		final ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();
		svc.schedule(window::close, 60, TimeUnit.SECONDS);

		gui.addWindowAndWait(window);
		screen.stopScreen();
		svc.shutdownNow();
	}

	@Test
	void testFileManagerLikeMC() {
		assertDoesNotThrow(() -> runMiniCommander(Paths.get(System.getProperty("user.dir"))));
	}
}