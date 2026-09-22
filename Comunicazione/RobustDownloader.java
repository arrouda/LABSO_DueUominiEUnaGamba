package Comunicazione;

import java.util.*;

/**
 * Classe per la gestione del download robusto di dati da nodi sensore.
 * Il downloader richiede all'aggregatore il token del nodo che possiede una misurazione,
 * tenta il download P2P e, in caso di esito positivo, rilascia il token.
 * In caso di errore/nodo irraggiungibile, avvisa l'aggregatore per aggiornare l'indice e riprova.
 */
public class RobustDownloader {

    private String ipAggregatore;
    private int portaAggregatore;

    public RobustDownloader(String ipAggregatore, int portaAggregatore) {
        this.ipAggregatore = ipAggregatore;
        this.portaAggregatore = portaAggregatore;
    }

    
    public String downloadWithRetry(String misurazione, String idNodoRichiedente) {
        return downloadWhitRetry(misurazione, idNodoRichiedente);
    }


    public String downloadWhitRetry(String misurazione, String idNodoRichiedente) {
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
            // Gestione flessibile della chiave ID sorgente ("idSensore" o "id")
            String idNodoSensore = token.getOrDefault("idSensore", token.get("id"));

            int portaNodoSensore;
            try {
                portaNodoSensore = Integer.parseInt(portaStr);
            } catch (NumberFormatException e) {
                System.err.println("Errore: formato porta non valido (" + portaStr + ") dal token. Interruzione.");
                break;
            }

            // Tenta il download P2P dal nodo sorgente
            download = tryDownload(ipNodoSensore, portaNodoSensore, misurazione);

            if (download != null && !download.isEmpty()) {
                downloadRiuscito = true;
                releaseToken(misurazione, idNodoSensore, downloadRiuscito);
            } else {
                // Se il download fallisce, notifica l'aggregatore per rimuovere l'entry e continua il ciclo
                System.err.println("Nodo " + (idNodoSensore != null ? idNodoSensore : ipNodoSensore) + " irraggiungibile. Riprovo...");
                notificaAggregatore(idNodoSensore, misurazione);
            }
        }

        return download;
    }

    /**
     * Rilascia il token comunicando all'aggregatore l'esito finale del download.
     */
    public void releaseToken(String misurazione, String idNodoSensore, boolean success) {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("misurazione", misurazione);
        parametri.put("idSensore", idNodoSensore != null ? idNodoSensore : "");
        parametri.put("success", Boolean.toString(success));

        String messaggio = GestoreMessaggi.serializedMessage("RELEASE_TOKEN", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
    }

    /**
     * Richiede all'aggregatore l'indirizzo e la porta del nodo sensore che possiede la misurazione.
     */
    private Map<String, String> requestToken(String misurazione, String idNodoRichiedente) {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("misurazione", misurazione);
        parametri.put("idRichiedente", idNodoRichiedente);

        String messaggio = GestoreMessaggi.serializedMessage("REQUEST_TOKEN", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        if (risposta == null || risposta.isEmpty()) {
            return null;
        }

        return GestoreMessaggi.parseMessage(risposta);
    }

    /**
     * Invia la richiesta di download P2P direttamente al nodo sensore sorgente.
     */
    private String tryDownload(String ipNodoSensore, int portaNodoSensore, String misurazione) {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("misurazione", misurazione);

        String messaggio = GestoreMessaggi.serializedMessage("DOWNLOAD", parametri);
        return NetworkClient.sendRequest(ipNodoSensore, portaNodoSensore, messaggio);
    }

    /**
     * Avvisa l'aggregatore che il nodo sorgente è risultato irraggiungibile o privo del dato.
     */
    private void notificaAggregatore(String idNodoSensore, String misurazione) {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("idSensore", idNodoSensore != null ? idNodoSensore : "");
        parametri.put("misurazione", misurazione);

        String messaggio = GestoreMessaggi.serializedMessage("NODE_FAILED", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
    }
}