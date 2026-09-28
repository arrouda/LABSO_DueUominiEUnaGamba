package Aggregatore;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
Logger gestisce la cronologia delle operazioni di download di rete.
Registra le richieste completate o fallite e fornisce l'elenco per il comando CLI log.
*/
public class Logger {

    private final List<String> logs = new ArrayList<>();
    //formatto data e ora Con il formato HH:mm 
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // il metodo è synchronized perchè più thread dell'aggregatore possono richiamare concorrentemente il logger.
    // per essere sicuro che il logger sia puntuale opero in sezione critica

    public synchronized void logOperation(String sourceNode, String destNode, String dataName, boolean success) {

        //aggiungo il momento in cui viene richmato il log
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        
        //aggiorno lo status del download (lo status dipende dal fatto che venga richiamto da NODE_FAILED o RELEASE_TOKEN)
        String status;

        if (success) {

            status = "";

        } else {

            status = " (FALLITO)";

        }
        
        //creo una stringa da aggiungere al log contenente la stringa con tutte le informazioni 
        String logEntry = timestamp + " " + dataName + " da: " + sourceNode + " a: " + destNode + status;

        logs.add(logEntry);

    }

    public synchronized List<String> getLogs() {

        return new ArrayList<>(logs);
    
    }
}