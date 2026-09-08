package Comunicazione;

import java.util.*;

/**
 * Classe che gestisce la formattazione dei messaggi da inviare e ricevere via socket.
 * Trasfroma i comandi e gli argomenti in stringhe per TCP e viceversa.
 */

public class GestoreMessaggi {

    // Devo prendere i comandi e gli argomenti e trasformarli in formato string
    // in modo che sia possibili inviarli via soket

    public static String serializedMessage(String command, Map<String, String> arguments) {

        // creo una Striga che rappresenti il comendo:
        String comando = "comando: " + command;

        String argomentiString = "";

        // Faccio un for-each per prendere tutti i parametri e li formatto come
        // striinga.

        // Uso una mappa <String, String> con chiave e valore perchè sia facile in
        // parseMessage estrarre i valori con la chiave e tornare ai parametri giusti

        for (Map.Entry<String, String> entry : arguments.entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue();

            argomentiString = argomentiString + ";" + key + "=" + value;

            // creo una stringa di coppie chiave-valore che sarà facile separare dopo
        }

        return comando + argomentiString;

        // ritorno una stringa formata dal comando e da tutti i parametri.

    }

    // Faccio il contrario che serializedMessage: prendo una stringa ed elaboro
    // una mappa di comandi e parametri

    public static Map<String, String> parseMessage(String messaggio) {

        Map<String, String> parametri = new HashMap<>();

        messaggio = messaggio.trim(); // elimino gli spazi

        if (messaggio.isEmpty()) {
            return parametri;
        }

        // Separo tutti i parametri tra loro in base al punto e virgola per ottenere
        // il comando e le coppie di parametro-valore.

        String[] argomenti = messaggio.split(";");

        // il primo parametro è sempre il comando.

        String comando = argomenti[0].substring("comando:".length());
        comando = comando.trim(); // rimuovo eventuali spazi
        parametri.put("comando", comando);

        // Per tutti i parametiri separo il parametro dal suo valore dove ho =
        // e lo aggiungo alla mappa

        for (int i = 1; i < argomenti.length; i++) {

            String argomento = argomenti[i];

            // mi assicuro di escludere eventuali valori nulli

            if (argomento.contains("=")) {
                String[] coppie = argomento.split("=", 2);
                parametri.put(coppie[0].trim(), coppie[1].trim());

            }
        }

        return parametri;
    }

}
