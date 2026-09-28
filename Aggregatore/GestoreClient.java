package Aggregatore;

import Comunicazione.*;
import java.io.*;
import java.net.*;
import java.util.*;

/*
GestoreClient gestisce il ciclo di vita della connessione socket TCP con un singolo sensore.
Viene eseguito su un thread dedicato per ogni client connesso all'Aggregatore.

Il thread:
- esegue run
- gestisce la richiesta con gestore richieste e invia la risposta
- chiude il socket  
- poi terminare run e muore
 */

public class GestoreClient implements Runnable {

    private final Socket clientSocket;
    private final Registro registro;
    private final Logger logger;


    public GestoreClient(Socket clientSocket, Registro registro, Logger logger) {

        this.clientSocket = clientSocket;
        this.registro = registro;
        this.logger = logger;

    }

    /*
    Gestisce il ciclo di vita della connessione socket con un singolo sensore.
    Legge la richiesta in ingresso, la interpreta tramite gestoreRichiesta e invia la risposta
     */
    
    @Override

    // Appena creato il thread entra nel metodo run per gestire la richiesta via rete del sensore
    // Anche in questo caso, come visto nella Comunicazione, usiamo BufferReader e PrinWriter per  
    public void run() {

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {

            //leggo la richiesta inviata dal Sensore e la passo al gestore richieste
            String richiesta = in.readLine();

            if (richiesta != null && !richiesta.trim().isEmpty()) {

                String risposta = gestoreRichiesta(richiesta);

                if (risposta != null) {
                    out.println(risposta);
                }
            }

        } catch (IOException e) {

            System.err.println("Errore di I/O durante la gestione del client: " + e.getMessage());

        } finally {

            // Dopo aver gestito la richiesta chiudo il socket

            try {

                if (clientSocket != null && !clientSocket.isClosed()) {

                    clientSocket.close();

                }

            } catch (IOException e) {

                System.err.println("Errore durante la chiusura della socket client: " + e.getMessage());

            }
        }

        // arrivato alla fine del metodo run il thread che gestiva la richiesta viene eliminato
    }

    

    /* 
    Gestore Richiesta è un metodo che gestisce tutti i comandi
    elabora le stringhe inviate via rete ed elaborate da gestore messaggi.
    l'azione dipende dal primo parametro della stringa che identifica un comando specifico da eseguire
    */

    private String gestoreRichiesta(String richiesta) {

        // trasformo la richiesta in mappa attravero il metodo di Gestore messaggi 
        // e salvo il comando in modo da poter svolgere l'azione relativa 

        Map<String, String> parametri = GestoreMessaggi.parseMessage(richiesta);
        String comando = parametri.get("comando");

        if (comando == null) {

            Map<String, String> err = new HashMap<>();
            err.put("status", "ERROR");
            err.put("messaggio", "Comando non specificato");
            return GestoreMessaggi.serializedMessage("RESPONSE", err);

        }

        Map<String, String> rispostaMap = new HashMap<>();

        switch (comando) {

            // il comando REGISTER serve per registrare un nuovo sensore all'interno del registro 
            // per farlo uso un oggetto InfoNodo che contiene tutte le informazione sul nodo (ip, porta e id)

            //Oltre a questo register viene richiamto ogni volta che devo aggiungere una rilevazione, 
            // quindi devo far si che il registro faccia differenza tra i due casi
            case "REGISTER": {

                // Estrazione dati di contatto e risorse possedute dal sensore
                String id = parametri.get("id");
                String ip = parametri.get("ip");
                String portaStr = parametri.get("porta");
                String misurazioniStr = parametri.get("misurazioni");

                if (id != null && ip != null && portaStr != null) {

                    int porta = Integer.parseInt(portaStr);
                    InfoNodo info = new InfoNodo(id, ip, porta);
                    List<String> listaMisurazioni;

                    if (misurazioniStr != null && !misurazioniStr.isEmpty()) {

                        listaMisurazioni = Arrays.asList(misurazioniStr.split(","));

                    } else {

                        // restiruisco una lista vuota
                        listaMisurazioni = null;

                    }

                    // Aggiornamento tabelle di registro centrale
                    registro.registerData(id, info, listaMisurazioni);
                    rispostaMap.put("status", "OK");

                } else {

                    rispostaMap.put("status", "ERROR");

                }

                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);

            }

            // REQUEST TOKEN è il comando usato nel downloadWithRetry per chiedere il token del nodo P2P da cui scaricare la risorsa
            // Chiede al registro il token per la rilevazione che desidero.
            // Se è disponibile lo restituisce, altrimenti restituisce un errore

            case "REQUEST_TOKEN": {

                // Richiesta di indirizzo del nodo sorgente per un determinato dato
                String misurazione = parametri.get("misurazione");
                String idRichiedente = parametri.get("idRichiedente");

                String tokenRes = registro.requestToken(misurazione, idRichiedente);

                if (tokenRes != null) {

                    // Formato stringa restituito da requestToken: "idSensore;ip;porta"
                    String[] parti = tokenRes.split(";");
                    rispostaMap.put("idSensore", parti[0]);
                    rispostaMap.put("ip", parti[1]);
                    rispostaMap.put("porta", parti[2]);
                    rispostaMap.put("status", "OK");

                } else {

                    rispostaMap.put("status", "ERROR");
                    rispostaMap.put("messaggio", "Nessun nodo disponibile per la risorsa");

                }

                // Risposta formattata compatibile con RobustDownloader
                return GestoreMessaggi.serializedMessage("TOKEN", rispostaMap);
            }

            //RELEASE TOKE viene richiamato solo in caso di download avvenuto con successo (se no viene mandato un NODE FAILED)


            case "RELEASE_TOKEN": {

                String misurazione = parametri.get("misurazione");
                String idSensore = parametri.get("idSensore");
                String idRichiedente = parametri.get("idRichiedente");
                boolean success = Boolean.parseBoolean(parametri.get("success"));

                // aggiorno il logger e invio la risposta con lo stato della richiesta
                if (logger != null) {
                    logger.logOperation(idSensore, idRichiedente, misurazione, success);
                }

                rispostaMap.put("status", "OK");
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }
            

            // NODE FAILED è invece il comando usato per comunicare che il token fornito non è valido

            // Qundo viene richiamato il NODE FAILED il logger registra il download come fallito
            // e rimuove dal registro la registrazione del nodo

            case "NODE_FAILED": {

                // Segnalazione di nodo sorgente irraggiungibile o privo della risorsa

                String idSensore = parametri.get("idSensore");
                String misurazione = parametri.get("misurazione");
                String idRichiedente = parametri.get("idRichiedente");

                if (idSensore != null && misurazione != null) {

                    registro.removeNodeForData(idSensore, misurazione);

                    if (logger != null) {
                        logger.logOperation(idSensore, idRichiedente != null ? idRichiedente : "", misurazione, false);
                    }

                    rispostaMap.put("status", "OK");

                } else {
                    rispostaMap.put("status", "ERROR");
                }

                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }


            // LISTDATA è il comando che viene usato per fare il listdata remote
            // restiruisce tutte le risorse presenti nel registro dell'aggregatore

            case "LISTDATA": {
                
                String dataName = parametri.get("misurazione");

                if (dataName != null && !dataName.isEmpty()) {

                    List<String> nodi = registro.getNodesForData(dataName);
                    rispostaMap.put("nodi", String.join(",", nodi));

                } else {
                    rispostaMap.put("risorse", registro.toString());
                }

                rispostaMap.put("status", "OK");
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }


            //Quit come da specifiche non rimuove il nodo

            case "QUIT": {

                // Recupera l'ID del nodo che si sta disconnettendo (estratto dai parametri del messaggio)
                String nodeId = parametri.get("id"); 

                if (nodeId != null) {
                    // 2. Rimuove il nodo e tutte le sue rilevazioni dal Registro
                    registro.removeNode(nodeId); 
                }

                //Prepara la risposta di conferma OK
                rispostaMap.put("status", "OK");
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }


            // Se la richiesta non è tra quelle precedente non sono in grado di gestirla e quindi restituisco un errore
            default:

                rispostaMap.put("status", "ERROR");
                rispostaMap.put("messaggio", "Comando non riconosciuto: " + comando);
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);

        }

    }
}
