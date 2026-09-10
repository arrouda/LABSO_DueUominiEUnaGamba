package Comunicazione;

import java.util.*;

/* 
 Classe per la gestione del download robusto di dati da nodi sensore.
 Il downloader chiede all'aggregatore il token del nodo che possiede una misurazione
 tenta il downlad e se ci riesce rilascia il token e comunica la rosposta

 Se no avvisa l'aggregatore che il nodo è irraggiungibile.

 il ciclo continua fino a che o non riesco a scaricare la misurazione o l'aggregatore non ha più nodi disponibili per quella misurazione
*/

public class RobustDownloader {

    public String ipAggregatore;
    public int portaAggregatore;


    public RobustDownloader(String IP, int porta) {
        this.ipAggregatore = IP;
        this.portaAggregatore = porta;
        
    }

    // Passo come parametro una stringa IdNodo e non direttamente il nodo 
    // perchè poi accedo all'oggetto InfoNodo e recupero IP e porta.

    // Passare la stringa e interrogare successivamente il registro mi facilita la trasmissione via socket.

    public String downloadWhitRetry(String Misurazione, String IdNodoRichiedente) {

        String download = "";
        boolean downloadRiuscito = false;

        while(!downloadRiuscito){

            Map <String, String> token = requestToken(Misurazione, IdNodoRichiedente);

            // se l'aggregatore non ha più nodi disponibili per la misurazione che sto cercando, 
            // ritorno un messaggio di errore e interrompo il ciclo

            if (token == null || !token.containsKey("ip")) {

                System.err.println("Download fallito: nessuna sorgente disponibile per " + Misurazione);
                break;

            }

            String ipNodoSensore = token.get("ip");
            int portaNodoSensore = Integer.parseInt(token.get("porta"));
            String idNodoSensore = token.get("idSensore");

            // una volta ottenuti i dati del nodo che potrebbe possedere la misurazizione provo a scaricarla

            download = tryDownload(ipNodoSensore, portaNodoSensore, Misurazione);

            if(download != null && !download.isEmpty()){

                downloadRiuscito = true;
                releaseToken(Misurazione, idNodoSensore, downloadRiuscito);

            } else {

                // se il download non è riuscito, rilascio il token al nodo sensore, comunico all'aggregatore e rimango nel ciclo while

                System.err.println("Nodo " + idNodoSensore + " irraggiungibile. Riprovo...");
                notificaAggregatore(idNodoSensore, Misurazione);

            }

        }

        return download;
    }

    

    public void releaseToken(String misurazione, String idNodoSensore, boolean success) {

        Map<String, String> parametri = new HashMap<>();
        parametri.put("misurazione", misurazione);
        parametri.put("idSensore", idNodoSensore);
        parametri.put("success", Boolean.toString(success));

        // formatto il messaggio in modo che sia inviabile all'aggregatore con serializedMessage di GestoreMessaggi

        String messaggio = GestoreMessaggi.serializedMessage("RELEASE_TOKEN", parametri);
        
        //invio la comunicazione all'aggregatore di rilascio del token, con il risultato del download (successo o fallimento)
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
    }




    // metodo per ottenere il token del nodo sensore che possiede la misurazione che voglio scaricare 

    private Map<String, String> requestToken(String Misurazione, String IdNodoRichiedente) {

        Map<String, String> parametri = new HashMap<>();

        parametri.put("misurazione", Misurazione);
        parametri.put("idRichiedente", IdNodoRichiedente);

        // uso il metodo in GestoreMessaggi per serializzare il messaggio da inviare all'aggregatore
        // serializedMessage trasforma la mappa e il comando in una stringa da inviare al server

        String messaggio = GestoreMessaggi.serializedMessage("REQUEST_TOKEN", parametri);

        // sendRequest apre un socket con l'aggregatore, invia il messaggio e riceve la risposta
        // salvo la risposta dell'aggregatore che conterrà il token

        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        // se non ho ricevuto risposta dall'aggregatore o se la risposta è vuota ritorno null
        // in questo modo gestisco il 
        if (risposta == null || risposta.isEmpty()) {
            return null;
        }

        // ricostruisco la mappa con i parametri del token (ip, id, porta) attraverso il metodo parseMessage di GestoreMessaggi
        Map<String, String> token = new HashMap<>();
        token = GestoreMessaggi.parseMessage(risposta);

        return token;

    }



    // metodo di download della misurazione

    private String tryDownload(String ipNodoSensore, int portaNodoSensore, String Misurazione) {

        // uso il metodo in GestoreMessaggi per serializzare il messaggio da inviare al nodo sensore
        // serializedMessage trasforma la mappa e il comando in una stringa da inviare al server

        Map<String, String> parametri = new HashMap<>();
        parametri.put("misurazione", Misurazione);

        String messaggio = GestoreMessaggi.serializedMessage("DOWNLOAD", parametri);

        // sendRequest apre un socket con il nodo sensore, invia il messaggio e riceve la risposta
        // salvo la risposta del nodo sensore che conterrà i dati della misurazione per poi ritornarla;

        String download = NetworkClient.sendRequest(ipNodoSensore, portaNodoSensore, messaggio);

        return download;

    }

    


    private void notificaAggregatore(String idNodoSensore, String misurazione) {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("idSensore", idNodoSensore);
        parametri.put("misurazione", misurazione);

        // formatto il messaggio in modo che sia inviabile all'aggregatore con serializedMessage di GestoreMessaggi

        String messaggio = GestoreMessaggi.serializedMessage("NODE_FAILED", parametri);

        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
    }


}
