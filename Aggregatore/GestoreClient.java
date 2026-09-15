package  Aggregatore;


import Comunicazione.GestoreMessaggi;
 
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
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
    // la connessione (con QUIT o per una disconnessione improvvisa)

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
 
        } else if (comando.equals("TOKEN")) {
            // il sensore chiede il permesso per scaricare una rilevazione:
            // il registro sceglie da quale nodo può prenderla (mai da se stesso)
            // e restituisce "id;ip;porta" di quel nodo, oppure null se non c'è
            String nomeDato = parametri.get("misurazione");
            String risultato = registro.requestToken(nomeDato, idNodo);
 
            // registriamo comunque il tentativo nel log, sia che sia andato
            // a buon fine sia che non ci fosse nessun nodo disponibile
            if (risultato == null) {
                log.logOperation("N/D", idNodo, nomeDato, false);
                return "NONE";
            }
 
            // risultato è già "id;ip;porta": lo rimandiamo così com'è,
            // il sensore che riceve sa come leggerlo
            String[] infoNodo = risultato.split(";");
            String idNodoTarget = infoNodo[0];
            log.logOperation(idNodoTarget, idNodo, nomeDato, true);
            return risultato;
 
        } else if (comando.equals("QUIT")) {
            // il sensore si sta disconnettendo volontariamente dalla rete
            registro.removeNode(idNodo);
            return "OK";
 
        } else {
            return "ERRORE comando sconosciuto";
        }
    }




}
