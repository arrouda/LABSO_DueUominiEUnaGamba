package Comunicazione;

import java.util.*;

/**
 * Classe che gestisce la formattazione e la decodifica dei messaggi da inviare e ricevere via socket TCP.
 * Trasforma comandi e argomenti in stringhe formattate di rete e viceversa.
 */
public class GestoreMessaggi {

    public static String serializedMessage(String command, Map<String, String> arguments) {
        
        StringBuilder sb = new StringBuilder("comando: ").append(command);

        if (arguments != null && !arguments.isEmpty()) {
            for (Map.Entry<String, String> entry : arguments.entrySet()) {
                sb.append(";").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        return sb.toString();
    }

    public static Map<String, String> parseMessage(String messaggio) {
        Map<String, String> parametri = new HashMap<>();

        if (messaggio == null) {
            return parametri;
        }

        messaggio = messaggio.trim();

        if (messaggio.isEmpty()) {
            return parametri;
        }

        // Separa l'intestazione del comando e i parametri tramite il separatore ";"
        String[] argomenti = messaggio.split(";");

        // Gestione difensiva del primo argomento (intestazione comando)
        if (argomenti[0].toLowerCase().startsWith("comando:")) {
            String comando = argomenti[0].substring("comando:".length()).trim();
            parametri.put("comando", comando);
        } else {
            // Fallback se il messaggio non contiene l'intestazione standard
            parametri.put("comando", argomenti[0].trim());
        }

        // Processa tutte le coppie chiave=valore successive
        for (int i = 1; i < argomenti.length; i++) {
            String argomento = argomenti[i];

            if (argomento.contains("=")) {
                String[] coppie = argomento.split("=", 2);
                parametri.put(coppie[0].trim(), coppie[1].trim());
            }
        }

        return parametri;
    }
}