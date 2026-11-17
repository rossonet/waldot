package net.rossonet.waldot.api.models;

/**
 * Severity OPC UA degli eventi di log mandati con
 * {@code AbstractOpcVertex.sendLogEvent(category, message, severity)}.
 * <p>
 * I valori cadono nelle fasce che waldot-plugin-syslog traduce nei livelli
 * syslog (0-99 Debug, 100-199 Info, 200-399 Notice, 400-599 Warning, 600-799
 * Error, 800-899 Critical, 900-999 Alert, 1000 Emergency).
 * <p>
 * Regola: un evento solo sulle transizioni (avvio, connessione persa o
 * ripristinata, configurazione applicata o rifiutata, failover), mai per ogni
 * campione, telegramma o ciclo. La category e' {@code <plugin>[-<area>]} in
 * minuscolo con trattino (es. {@code mp104-cluster}, {@code iec104-command}).
 */
public final class LogEventSeverity {

	/** Guasto che richiede intervento immediato (firewall in panic). */
	public static final int ALERT = 950;
	/**
	 * Guasto hardware o di sicurezza (alimentazione, CPU, firewalld
	 * indisponibile, health DOWN).
	 */
	public static final int CRITICAL = 850;
	/**
	 * Proprieta' dei vertici di punto (registri, comandi, ingressi): se
	 * {@code true} il vertice manda un evento di log NOTICE a ogni cambio di
	 * valore (con il limite di frequenza del plugin). Si attiva su richiesta,
	 * insieme a un arco fire verso un mittente syslog (muuh clarabella mp104
	 * syslog link).
	 */
	public static final String EVENT_ON_DATA_CHANGE_LABEL = "set!event-on-data-change";
	/** Valore di debug (non arriva al syslog di clarabella). */
	public static final int DEBUG = 50;
	/**
	 * Funzione persa: connessione persa, avvio fallito, comando rifiutato,
	 * overflow, container fermo inatteso.
	 */
	public static final int ERROR = 650;
	/** Dettaglio utile ma non necessario (filtrato dal syslog di clarabella). */
	public static final int INFO = 150;
	/**
	 * Transizione normale: avviato, connesso, ripristinato, configurazione
	 * applicata, comando eseguito, cambio di leader.
	 */
	public static final int NOTICE = 250;
	/**
	 * Anomalia gestita: failover, riavvio automatico, configurazione rifiutata,
	 * connessione chiusa, risorsa vicina al limite.
	 */
	public static final int WARNING = 500;

	private LogEventSeverity() {
	}
}
