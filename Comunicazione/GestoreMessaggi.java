package Comunicazione;

import java.util.HashMap;
import java.util.Map;

/*
Con questa classe Codifico e decodifico i messaggi in modo da poterli inviare via socket, 
definendo delle regole comuni, usate dalle altre classi per sfruttare le comunicazioni via socket.
 
Partendo da un comando e una mappa di comandi creo un unica stringa inviabile via socket.

Nel momento in cui ricevo la stringa, conoscendo la forma e il nome delle chiavi della mappa 
sarò in grado di risalire all'azione e ai parametri

 */

public class GestoreMessaggi {


    // metodo che trasfroma i messaggi prima di inviarli

    public static String serializedMessage(String command, Map<String, String> arguments) {

        String sb = "comando:" + command;

        if (arguments != null && !arguments.isEmpty()) {

            // prendo la mappa e per ogni elemento della mappa salvo la chiave e il valore separatamente
            // in questo modo conoscendo e concordando sulle chiavi posso risalire velocemente ai valori

            for (Map.Entry<String, String> entry : arguments.entrySet()) {

            String val;

                if (entry.getValue() != null) {
                    
                    //maschero il punto e virgola e a capo in modo che non si confonda in parseMessage;
                    val = entry.getValue().replace(";", "%3B");
                    val = val.replace("\n", "%0A");

                } else {

                    val = "";

                }

                sb = sb + ";" + entry.getKey() + "=" + val;
            }
        }

        return sb;
    }



    // partendo dalla stringa ricorstruisco la mappa di comando-valori iniziale;

    public static Map<String, String> parseMessage(String messaggio) {

        Map<String, String> parametri = new HashMap<>();

        if (messaggio == null || messaggio.isEmpty()) {

            return parametri;

        }

        // devo farlo dopo se no mi lancia l'eccezione, perchè trim non può essere fatto su un valore null
        messaggio = messaggio.trim();

        String[] argomenti = messaggio.split(";");


        // prendo il primo elemento della stringa e ne estrapolo il comando 
        // gestendo il caso in cui io abbia un'intestazione sbagliata

        if (argomenti[0].toLowerCase().startsWith("comando:")) {

            String comando = argomenti[0].substring("comando:".length()).trim();
            parametri.put("comando", comando);
            
        } else if (argomenti[0].toLowerCase().startsWith("comando")) {

            String comando = argomenti[0].substring("comando".length()).trim();
            parametri.put("comando", comando);

        } else {

            parametri.put("comando", argomenti[0].trim());

        }


        // Processa tutte le coppie chiave=valore successive aggiornando la mappa

        for (int i = 1; i < argomenti.length; i++) {

            String argomento = argomenti[i];

            if (argomento.contains("=")) {

                // divido le stringhe all'uguale e metto la prima parte come chiave e la seconda come valore
                
                String[] coppie = argomento.split("=", 2);
                String val = coppie[1].trim().replace("%3B", ";").replace("%0A", "\n");
                parametri.put(coppie[0].trim(), val);

            }
        }

        return parametri;
    }
}