package Aggregatore;


import Comunicazione.GestoreMessaggi;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GestoreClient implements Runnable {


      private Socket socket;
      private Registro registro;
      private Logger log;
      private String idNodo; // resta null finché il sensore non si registra con REGISTER


   public GestoreClient(Socket socket, Registro registro, Logger log) {
        this.socket = socket;
        this.registro = registro;
        this.log = log;
    }

    // ogni sensore connesso ha la sua istanza di GestoreClient su un thread
    // dedicato: qui si legge una riga alla volta finché il client non chiude
    // la connessione (con QUIT o per una disconnessione improvvisa).
    //
    // Nota: non tutte le connessioni sono "sensori registrati" di lunga
    // durata. RobustDownloader (package Comunicazione) usa NetworkClient,
    // che apre una connessione nuova e la chiude subito dopo ogni singola
    // richiesta (REQUEST_TOKEN, RELEASE_TOKEN, NODE_FAILED): per queste
    // connessioni idNodo resta sempre null, perché non passano mai da
    // REGISTER su questo socket.

    @Override
   public void run() {
      // getsisce il cilco di vita della connessione socket con un sinoglo sensore
      // legge messaggi in ingresso li interpreta e invia le risposte.

      try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String riga;
            while ((riga = in.readLine()) != null) {
                // per ogni riga ricevuta, calcoliamo la risposta e la rimandiamo subito
                String risposta = gestoreRichiesta(riga);
                out.println(risposta);
            }

            socket.close();
        } catch (IOException e) {
            // la connessione è caduta senza un QUIT esplicito (crash del client,
            // rete che salta, ecc.): il nodo va comunque tolto dal registro,
            // altrimenti l'aggregatore continuerebbe a proporlo agli altri
            if (idNodo != null) {
                registro.removeNode(idNodo);
            }
        }
    }

    // messaggio tipo "comando: REGISTER;id=nodo1;misurazioni=R0,R1;ip=...;porta=..."
    // GestoreMessaggi.parseMessage lo trasforma in una mappa con "comando" +
    // tutte le altre coppie chiave-valore (nomi di Roda: id, misurazione, ip, porta)


private String gestoreRichiesta(String richiesta) {
        Map<String, String> parametri = GestoreMessaggi.parseMessage(richiesta);
        String comando = parametri.get("comando");

        if (comando == null) {
            // messaggio vuoto o malformato (parseMessage non trova "comando: ...")
            return "ERRORE comando mancante";
        }

        if (comando.equals("REGISTER")) {
            // il sensore comunica chi è, quali rilevazioni possiede e dove si trova
            // (ip e porta servono a chi vorrà scaricare da lui più avanti)
            idNodo = parametri.get("id");
            List<String> dati = Arrays.asList(parametri.get("misurazioni").split(","));
            String ip = parametri.get("ip");
            String porta = parametri.get("porta");
            registro.registerData(idNodo, dati, ip, porta);
            return "OK";

        } else if (comando.equals("NODES_FOR")) {
            // il sensore vuole sapere chi altro possiede una certa rilevazione
            String nomeDato = parametri.get("misurazione");
            List<String> nodi = registro.getNodesForData(nomeDato);
            return String.join(",", nodi);

        } else if (comando.equals("TOKEN") || comando.equals("REQUEST_TOKEN")) {
            // "TOKEN" arriva da un sensore già registrato su questa stessa
            // connessione (usiamo l'idNodo di sessione impostato da REGISTER).
            // "REQUEST_TOKEN" arriva invece da RobustDownloader, che apre una
            // connessione usa-e-getta per ogni richiesta e quindi non ha mai
            // fatto REGISTER qui: l'unico modo per sapere chi sta chiedendo è
            // leggere "idRichiedente" dal messaggio stesso.
            String nomeDato = parametri.get("misurazione");
            String richiedente = parametri.containsKey("idRichiedente")
                    ? parametri.get("idRichiedente")
                    : idNodo;

            // il registro sceglie da quale nodo si può scaricare la rilevazione
            // (mai dal richiedente stesso) e restituisce "id;ip;porta", o null
            // se non c'è nessun nodo disponibile
            String risultato = registro.requestToken(nomeDato, richiedente);

            if (risultato == null) {
                // qui l'esito è già definitivo (nessuna sorgente disponibile),
                // quindi lo registriamo subito nel log
                log.logOperation("N/D", richiedente, nomeDato, false);
                return GestoreMessaggi.serializedMessage("NONE", new HashMap<>());
            }

            // risultato è "id;ip;porta" così come lo produce Registro: lo
            // ricodifichiamo nel formato di GestoreMessaggi, che è quello che
            // RobustDownloader si aspetta di poter riparsare con
            // GestoreMessaggi.parseMessage (chiavi idSensore/ip/porta)
            String[] infoNodo = risultato.split(";");
            Map<String, String> risposta = new HashMap<>();
            risposta.put("idSensore", infoNodo[0]);
            risposta.put("ip", infoNodo[1]);
            risposta.put("porta", infoNodo[2]);

            // non logghiamo ancora un successo qui: a questo punto sappiamo
            // solo che un token è stato assegnato, non che il download sia
            // davvero riuscito. L'esito vero arriva dopo, con RELEASE_TOKEN
            // (successo) o NODE_FAILED (fallimento reale del nodo sorgente).
            return GestoreMessaggi.serializedMessage("TOKEN", risposta);

        } else if (comando.equals("RELEASE_TOKEN")) {
            // il downloader comunica l'esito reale del download: è questo il
            // momento giusto per scrivere il log, non l'assegnazione del token
            String nomeDato = parametri.get("misurazione");
            String idSensore = parametri.get("idSensore");
            boolean successo = Boolean.parseBoolean(parametri.get("success"));
            // RobustDownloader non include l'id del richiedente in questo
            // messaggio (è una connessione nuova, senza REGISTER pregresso),
            // quindi qui non possiamo risalire a chi ha fatto la richiesta
            log.logOperation(idSensore, "N/D", nomeDato, successo);
            return "OK";

        } else if (comando.equals("NODE_FAILED")) {
            // il nodo sorgente non ha risposto al download: lo togliamo dal
            // registro solo per questa rilevazione (potrebbe averne altre
            // valide, quindi non lo rimuoviamo del tutto) e registriamo il
            // fallimento reale nel log
            String nomeDato = parametri.get("misurazione");
            String idSensore = parametri.get("idSensore");
            registro.removeNodeForData(idSensore, nomeDato);
            log.logOperation(idSensore, "N/D", nomeDato, false);
            return "OK";

        } else if (comando.equals("QUIT")) {
            // il sensore si sta disconnettendo volontariamente dalla rete
            registro.removeNode(idNodo);
            return "OK";

        } else {
            return "ERRORE comando sconosciuto";
        }
    }




}