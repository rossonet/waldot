package net.rossonet.waldot.cli.commands;

import java.util.List;

import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;

/**
 * {@code waldot about}: server information.
 */
@Command(name = "about", header = "Informazioni sul server WaldOT (anche verifica della connessione).", mixinStandardHelpOptions = true, description = {
		"Mostra le informazioni sul server WaldOT (comando 'about': versione, namespace,",
		"plugin e configurazione principale). Utile anche come verifica della connessione:",
		"il codice di uscita è 0 se il server risponde, 3 se non è raggiungibile." }, footer = {
				"Output:", "  text, value: una riga per ogni riga della risposta;",
				"  json: array di stringhe; jsonl: una stringa JSON per riga.", "", "Esempi:",
				"  waldot about", "  waldot about --endpoint opc.tcp://192.168.1.10:12686/waldot",
				"  waldot about --connect-timeout 3s >/dev/null && echo raggiungibile" })
public class AboutCommand extends ClientCommand {

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final List<String> lines = client.commands().about();
		printer.list(lines, line -> line, line -> line, line -> line);
		return 0;
	}
}
