import java.util.List;

public class Logger {

    public synchronized void logOperation(String sourceNode, String destNode, String dataName, boolean success) {
    // aggiunge una voce al log registrando timestamp, nodi coinvolti,
    // rilevazione scaricata ed esito del download
    }
 
    public synchronized List<String> getLogs() {
        // restituisce l'elenco formattato dei log per la stampa del comando log
        return null;
    }

}
