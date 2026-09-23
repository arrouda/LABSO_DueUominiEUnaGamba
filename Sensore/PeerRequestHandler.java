package sensore;

import comunicazione.*;

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
            // Lettura del messaggio di richiesta inviato dal client P2P
            String richiesta = reader.readLine();
            if (richiesta == null || richiesta.trim().isEmpty()) {
                return;
            }

            // Decodifica della richiesta ricevuta
            Map<String, String> parametri = GestoreMessaggi.parseMessage(richiesta);
            String comando = parametri.get("comando");
            String nomeRisorsa = parametri.getOrDefault("risorsa", parametri.get("misura"));

            Map<String, String> rispostaParametri = new HashMap<>();

            if ("DOWNLOAD".equalsIgnoreCase(comando) && nomeRisorsa != null) {
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

            // Serializzazione della risposta e invio sulla socket
            String messaggioRisposta = GestoreMessaggi.serializedMessage("RESPONSE", rispostaParametri);
            writer.println(messaggioRisposta);

        } catch (IOException e) {
            System.err.println("[PEER HANDLER ERRORE] Errore durante il trasferimento dati P2P: " + e.getMessage());
        } finally {
            // Chiusura garantita della socket al termine dell'operazione
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
