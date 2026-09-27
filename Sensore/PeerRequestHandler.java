package Sensore;

import Comunicazione.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class PeerRequestHandler implements Runnable {

    private final Socket socket;
    private final ArchivioLocale archivio;


    public PeerRequestHandler(Socket socket, ArchivioLocale archivio) {

        this.socket = socket;
        this.archivio = archivio;

    }



    @Override
    public void run() {

        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {

            // Leggo la richiesta che arriva dalla rete
            String richiesta = reader.readLine();

            if (richiesta == null || richiesta.trim().isEmpty()) {
                return;
            }

            // Decodifica della richiesta ricevuta
            Map<String, String> parametri = GestoreMessaggi.parseMessage(richiesta);
            String comando = parametri.get("comando");
            String nomeRisorsa = parametri.getOrDefault("risorsa", parametri.getOrDefault("misura", parametri.get("misurazione")));

            Map<String, String> rispostaParametri = new HashMap<>();

            if ("DOWNLOAD".equalsIgnoreCase(comando) && nomeRisorsa != null) {
                // Di base mi aspetto solo il download come comando, 
                // Però metto comunuqe la condizione perchè devo gestire il caso in cui ho un comando inaspettato

                // Recupero del contenuto della rilevazione dall'Archivio Locale
                String contenuto = archivio.getContent(nomeRisorsa);

                if (contenuto != null) {

                    rispostaParametri.put("status", "OK");
                    rispostaParametri.put("contenuto", contenuto);
                    rispostaParametri.put("risorsa", nomeRisorsa);

                } else {

                    rispostaParametri.put("status", "ERROR");
                    rispostaParametri.put("messaggio", "Risorsa non trovata nell'archivio locale");

                }

            } else {

                rispostaParametri.put("status", "ERROR");
                rispostaParametri.put("messaggio", "Comando non valido o parametro risorsa mancante");

            }

            // Elaboro la risposta secondo il metodo di serializzazione per inviare il comando via rete 
            // e scrivo la risposta sul socket così da inviarla

            String messaggioRisposta = GestoreMessaggi.serializedMessage("RESPONSE", rispostaParametri);
            writer.println(messaggioRisposta);

        } catch (IOException e) {

            System.err.println("[PEER HANDLER ERRORE] Errore durante il trasferimento dati P2P: " + e.getMessage());

        } finally {
            
            // Chiudo il socket in qualsiasi caso
            try {

                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

            } catch (IOException e) {

                System.err.println("[PEER HANDLER ERRORE] Impossibile chiudere la socket: " + e.getMessage());

            }
        }
    }
}