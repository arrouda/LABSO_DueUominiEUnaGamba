package Comunicazione;

import java.util.HashMap;
import java.util.Map;

/*
Classe per la gestione del download robusto di dati da nodi sensore.
Il downloader richiede all'aggregatore il token del nodo che possiede una misurazione,
tenta il download P2P e, in caso di esito positivo, rilascia il token.
In caso di errore/nodo irraggiungibile, avvisa l'aggregatore per aggiornare l'indice e riprova.
 */

public class RobustDownloader {

    private String ipAggregatore;
    private int portaAggregatore;

    public RobustDownloader(String ipAggregatore, int portaAggregatore) {

        this.ipAggregatore = ipAggregatore;
        this.portaAggregatore = portaAggregatore;

    }



    /*
    metodo per fare il download e riprovare fino a che o non mancano più nodi (in quel caso con il break si chiude)
    o fino a quando il download non finisce correttamente e allora la variabile di controllo cambia e restiruisco il contenuto del download

    Nel caso in cui il nodo si è disconnesso e io ho ancora la rilevazione nell'aggregatore, 
    richiamo un metodo che avvisa l'aggregatore il quale rimuoverà la rilevazione dal registro.
    */

    public String downloadWithRetry(String misurazione, String idNodoRichiedente) {

        String download = null;
        boolean downloadRiuscito = false;

        while (!downloadRiuscito) {

            Map<String, String> token = requestToken(misurazione, idNodoRichiedente);

            // Se l'aggregatore non ha più nodi disponibili o la risposta non è valida, interrompe il ciclo

            if (token == null || !token.containsKey("ip") || !token.containsKey("porta")) {
                System.err.println("Download fallito: nessuna sorgente disponibile per " + misurazione);
                break;

            }

            String ipNodoSensore = token.get("ip");
            String portaStr = token.get("porta");

            // Gestione flessibile della chiave ID sorgente ("idSensore" o "id") per sicurezza perchè abbiamo usato nomi diversi
            String idNodoSensore = token.getOrDefault("idSensore", token.get("id"));
            int portaNodoSensore;
            
            // Siccome la porta inialmente viene fornita come stringa devo fare un parseInt;
            try {

                portaNodoSensore = Integer.parseInt(portaStr);

            } catch (NumberFormatException e) {

                System.err.println("Errore: formato porta non valido (" + portaStr + ") dal token. Interruzione.");
                break;

            }

            // Tenta il download P2P dal nodo sorgente che gli è stato restituito

            String rispostaStr = tryDownload(ipNodoSensore, portaNodoSensore, misurazione);
            Map<String, String> rispostaMap = GestoreMessaggi.parseMessage(rispostaStr);


            // se lo status del download va bene allora aggiungo il contenuto alla variabile download
            // ed esco dal ciclo settando la variabile del while a true

            if ("OK".equalsIgnoreCase(rispostaMap.get("status"))) {

                download = rispostaMap.get("contenuto");
                downloadRiuscito = true;
                releaseToken(misurazione, idNodoSensore, idNodoRichiedente, downloadRiuscito);

            } else {

                // Se il download fallisce, notifica l'aggregatore per rimuovere l'entry e continua il ciclo
                System.err.println("Nodo " + (idNodoSensore != null ? idNodoSensore : ipNodoSensore) + " irraggiungibile. Riprovo...");
                notificaAggregatore(idNodoSensore, misurazione, idNodoRichiedente);

            }
        }

        return download;
    }



    
    // Rilascia il token comunicando all'aggregatore l'esito finale del download in modo che lo possa mettere nel logger

    public void releaseToken(String misurazione, String idNodoSensore, String idNodoRichiedente, boolean success) {

        Map<String, String> parametri = new HashMap<>();

        parametri.put("misurazione", misurazione);
        parametri.put("idSensore", idNodoSensore);
        parametri.put("idRichiedente", idNodoRichiedente);
        parametri.put("success", Boolean.toString(success));

        String messaggio = GestoreMessaggi.serializedMessage("RELEASE_TOKEN", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

    }


    
    // Richiede all'aggregatore l'indirizzo e la porta del nodo sensore che possiede la misurazione.
    private Map<String, String> requestToken(String misurazione, String idNodoRichiedente) {

        Map<String, String> parametri = new HashMap<>();

        parametri.put("misurazione", misurazione);
        parametri.put("idRichiedente", idNodoRichiedente);

        // Uso il metodo di GestoreMessaggi e NetworkClient per creare la richiesta con il comando REQUEST_TOKE, gestito da GestoreClient
        String messaggio = GestoreMessaggi.serializedMessage("REQUEST_TOKEN", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        if (risposta == null || risposta.isEmpty()) {
            return null;
        }
 
        return GestoreMessaggi.parseMessage(risposta);
    }

   
    

    // Invia la richiesta di download P2P direttamente al nodo sensore sorgente.
    private String tryDownload(String ipNodoSensore, int portaNodoSensore, String misurazione) {
        Map<String, String> parametri = new HashMap<>();

        parametri.put("misurazione", misurazione);

         // Uso il metodo di GestoreMessaggi e NetworkClient per creare la richiesta con il comando DOWNLOAD, gestito da PeerRequestHandler
        String messaggio = GestoreMessaggi.serializedMessage("DOWNLOAD", parametri);
        String risposta = NetworkClient.sendRequest(ipNodoSensore, portaNodoSensore, messaggio);

        return risposta;

    }

    

    // Metodo che avvisa l'aggregatore solo nel caso di problemi nel download
    
    private void notificaAggregatore(String idNodoSensore, String misurazione, String idNodoRichiedente) {

        Map<String, String> parametri = new HashMap<>();

        parametri.put("idSensore", idNodoSensore != null ? idNodoSensore : "");
        parametri.put("misurazione", misurazione);
        parametri.put("idRichiedente", idNodoRichiedente != null ? idNodoRichiedente : "");

        // il comando NODE_FAILED Avvisa l'aggregatore che il nodo sorgente è risultato irraggiungibile o privo del dato.
        String messaggio = GestoreMessaggi.serializedMessage("NODE_FAILED", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

    }
}