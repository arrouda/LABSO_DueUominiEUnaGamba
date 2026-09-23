package Aggregatore;

import Comunicazione.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * GestoreClient gestisce il ciclo di vita della connessione socket TCP con un singolo sensore.
 * Viene eseguito su un thread dedicato per ogni client connesso all'Aggregatore.
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

    /**
     * Gestisce il ciclo di vita della connessione socket con un singolo sensore[cite: 2].
     * Legge la richiesta in ingresso, la interpreta tramite gestoreRichiesta e invia la risposta[cite: 2].
     */
    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
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
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Errore durante la chiusura della socket client: " + e.getMessage());
            }
        }
    }

    
    private String gestoreRichiesta(String richiesta) {
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
            case "REGISTER": {
                // Estrazione dati di contatto e risorse possedute dal sensore[cite: 1]
                String id = parametri.get("id");
                String ip = parametri.get("ip");
                String portaStr = parametri.get("porta");
                String misurazioniStr = parametri.get("misurazioni");

                if (id != null && ip != null && portaStr != null) {
                    int porta = Integer.parseInt(portaStr);
                    InfoNodo info = new InfoNodo(id, ip, porta);
                    List<String> listaMisurazioni = (misurazioniStr != null && !misurazioniStr.isEmpty())
                            ? Arrays.asList(misurazioniStr.split(","))
                            : List.of();

                    // Aggiornamento tabelle di registro centrale[cite: 1, 2]
                    registro.registerData(id, info, listaMisurazioni);
                    rispostaMap.put("status", "OK");
                } else {
                    rispostaMap.put("status", "ERROR");
                }
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }

            case "REQUEST_TOKEN": {
                // Richiesta di indirizzo del nodo sorgente per un determinato dato[cite: 1, 6]
                String misurazione = parametri.get("misurazione");
                String idRichiedente = parametri.get("idRichiedente");

                String tokenRes = registro.requestToken(misurazione, idRichiedente);
                if (tokenRes != null) {
                    // Formato stringa restituito da requestToken: "idSensore;ip;porta"[cite: 1]
                    String[] parti = tokenRes.split(";");
                    rispostaMap.put("idSensore", parti[0]);
                    rispostaMap.put("ip", parti[1]);
                    rispostaMap.put("porta", parti[2]);
                    rispostaMap.put("status", "OK");
                } else {
                    rispostaMap.put("status", "ERROR");
                    rispostaMap.put("messaggio", "Nessun nodo disponibile per la risorsa");
                }
                // Risposta formattata compatibile con RobustDownloader[cite: 6]
                return GestoreMessaggi.serializedMessage("TOKEN", rispostaMap);
            }

            case "RELEASE_TOKEN": {
                // Notifica di esito download ed eventuale tracciamento sul Logger[cite: 1, 2, 6]
                String misurazione = parametri.get("misurazione");
                String idSensore = parametri.get("idSensore");
                String idRichiedente = parametri.get("idRichiedente");
                boolean success = Boolean.parseBoolean(parametri.get("success"));

                if (logger != null) {
                    logger.logOperation(idSensore, idRichiedente, misurazione, success);
                }

                rispostaMap.put("status", "OK");
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }

            case "NODE_FAILED": {
                // Segnalazione di nodo sorgente irraggiungibile o privo della risorsa[cite: 1, 6]
                String idSensore = parametri.get("idSensore");
                String misurazione = parametri.get("misurazione");

                if (idSensore != null && misurazione != null) {
                    registro.removeNodeForData(idSensore, misurazione);
                    rispostaMap.put("status", "OK");
                } else {
                    rispostaMap.put("status", "ERROR");
                }
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }

            case "NODES_FOR":
            case "LISTDATA": {
                // Consultazione delle risorse presenti sul registro globale[cite: 1, 3]
                String dataName = parametri.get("misurazione");
                if (dataName != null && !dataName.isEmpty()) {
                    List<String> nodi = registro.getNodesForData(dataName);
                    rispostaMap.put("nodi", String.join(",", nodi));
                } else {
                    rispostaMap.put("registro", registro.toString());
                }
                rispostaMap.put("status", "OK");
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }

            case "QUIT": {
                // Disconnessione pulita di un sensore[cite: 1, 2]
                String id = parametri.get("id");
                if (id != null) {
                    registro.removeNode(id);
                }
                rispostaMap.put("status", "OK");
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
            }

            default:
                rispostaMap.put("status", "ERROR");
                rispostaMap.put("messaggio", "Comando non riconosciuto: " + comando);
                return GestoreMessaggi.serializedMessage("RESPONSE", rispostaMap);
        }
    }
}
