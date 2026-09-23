package Aggregatore;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Logger gestisce la cronologia thread-safe delle operazioni di download di rete.
 * Registra le richieste completate o fallite e fornisce l'elenco per il comando CLI log.
 */
public class Logger {

    private final List<String> logs = new ArrayList<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");


    public synchronized void logOperation(String sourceNode, String destNode, String dataName, boolean success) {

        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String status = success ? "" : " (FALLITO)";
        
        // Formato conforme alle specifiche: "13:00 R0 da: peer0 a: peer1"[cite: 2]
        String logEntry = String.format("%s %s da: %s a: %s%s", 
                timestamp, dataName, sourceNode, destNode, status);
        
        logs.add(logEntry);

    }

    public synchronized List<String> getLogs() {

        return new ArrayList<>(logs);
    
    }
}