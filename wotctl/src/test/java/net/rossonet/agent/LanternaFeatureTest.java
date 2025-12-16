package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.ProgressBar;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.dialogs.FileDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LanternaFeatureTest {

	private static MultiWindowTextGUI gui;
	private static Screen screen;
	private static Terminal terminal;

	@BeforeAll
	static void setup() throws IOException {
		// Terminal & Screen
		terminal = new DefaultTerminalFactory().setForceTextTerminal(true).createTerminal();
		screen = new TerminalScreen(terminal);
		screen.startScreen();
		gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.RED));
	}

	@AfterAll
	static void tearDown() {
		try {
			gui.getGUIThread().invokeAndWait(() -> {
				gui.getActiveWindow().close();
			});
			screen.stopScreen();
			terminal.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/* ---------- Dialoghi pre-costruiti ---------- */
	@Test
	@Order(4)
	void testDialogs() {
		assertDoesNotThrow(() -> gui.getGUIThread().invokeAndWait(
				() -> MessageDialog.showMessageDialog(gui, "Titolo", "Messaggio di prova", MessageDialogButton.OK)));

		assertDoesNotThrow(() -> gui.getGUIThread().invokeAndWait(
				() -> ListSelectDialog.showDialog(gui, "Seleziona", "Scegli un valore", Arrays.asList("A", "B", "C"))));
	}

	/* ---------- FileDialog (richiede accesso FS) ---------- */
	@Test
	@Order(5)
	void testFileDialog() {
		assertDoesNotThrow(() -> gui.getGUIThread()
				.invokeAndWait(() -> new FileDialogBuilder().setTitle("Apri file").build().showDialog(gui)));
	}

	/* ---------- Layer 3: GUI2 – Widget, Layout, Eventi ---------- */
	@Test
	@Order(3)
	void testGuiComponents() {
		assertDoesNotThrow(() -> {
			final BasicWindow win = new BasicWindow("Demo GUI");
			final Panel panel = new Panel(new GridLayout(2));

			panel.addComponent(new Label("Nome:"));
			final TextBox name = new TextBox().addTo(panel);

			panel.addComponent(new Label("Password:"));
			final TextBox pwd = new TextBox().setMask('*').addTo(panel);

			final Button ok = new Button("OK", win::close);
			panel.addComponent(new EmptySpace());
			panel.addComponent(ok);

			win.setComponent(panel);
			gui.addWindowAndWait(win); // blocca finché la finestra è aperta
		});
	}

	/* ---------- Concurrency & ProgressBar ---------- */
	@Test
	@Order(6)
	void testProgressBar() {
		assertDoesNotThrow(() -> {
			final BasicWindow win = new BasicWindow("Task");
			final Panel p = new Panel();
			final ProgressBar bar = new ProgressBar();
			p.addComponent(bar);
			win.setComponent(p);
			gui.addWindow(win);

			Executors.newSingleThreadExecutor().submit(() -> {
				for (int i = 0; i <= 100; i++) {
					final int val = i;
					gui.getGUIThread().invokeLater(() -> bar.setValue(val));
					try {
						Thread.sleep(20);
					} catch (final InterruptedException ignored) {
					}
				}
				gui.getGUIThread().invokeLater(win::close);
			});
			gui.waitForWindowToClose(win);
		});
	}

	/* ---------- Layer 2: Screen & TextGraphics ---------- */
	@Test
	@Order(2)
	void testScreenDrawing() {
		assertDoesNotThrow(() -> {
			final TextGraphics tg = screen.newTextGraphics();
			tg.setForegroundColor(TextColor.ANSI.YELLOW);
			tg.putString(2, 2, "Hello Screen!");
			tg.drawRectangle(new TerminalPosition(0, 0), new TerminalSize(20, 5), '+');
			screen.refresh(); // double-buffer → terminal
		});
	}

	/* ---------- Layer 1: Terminal ---------- */
	@Test
	@Order(1)
	void testTerminalIO() {
		assertDoesNotThrow(() -> {
			terminal.putCharacter('L');
			terminal.putCharacter('a');
			terminal.flush();
			terminal.resetColorAndSGR();
		});
	}
}