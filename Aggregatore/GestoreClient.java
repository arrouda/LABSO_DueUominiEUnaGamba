import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;


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
 
    // formato dei messaggi (righe di testo semplici, comando + argomenti separati da spazio):
    //   REGISTER <idNodo> <dato1,dato2,...>
    //   NODES_FOR <nomeDato>
    //   TOKEN <nomeDato>
    //   QUIT


   private String gestoreRichiesta(String richiesta) {
      // decodifica le richiesta ricevuta (registrati, chiedi lista, richiedi token)
      // e invoca i relativi metodi del Registro o Logger

      String[] parti = richiesta.split(" ");
        String comando = parti[0];
 
        if (comando.equals("REGISTER")) {
            // il sensore comunica chi è e quali rilevazioni possiede;
            // salviamo l'id qui perché serve anche più avanti (es. in QUIT)
            idNodo = parti[1];
            List<String> dati = Arrays.asList(parti[2].split(","));
            registro.registerData(idNodo, dati);
            return "OK";
 
        } else if (comando.equals("NODES_FOR")) {
            // il sensore vuole sapere chi altro possiede una certa rilevazione
            List<String> nodi = registro.getNodesForData(parti[1]);
            return String.join(",", nodi);
 
        } else if (comando.equals("TOKEN")) {
            // il sensore chiede il permesso per scaricare una rilevazione:
            // il registro sceglie da quale nodo può prenderla (mai da se stesso)
            String nomeDato = parti[1];
            String nodo = registro.requestToken(nomeDato, idNodo);
 
            // registriamo comunque il tentativo nel log, sia che sia andato
            // a buon fine sia che non ci fosse nessun nodo disponibile
            if (nodo == null) {
                log.logOperation("N/D", idNodo, nomeDato, false);
                return "NONE";
            }
            log.logOperation(nodo, idNodo, nomeDato, true);
            return nodo;
 
        } else if (comando.equals("QUIT")) {
            // il sensore si sta disconnettendo volontariamente dalla rete
            registro.removeNode(idNodo);
            return "OK";
 
        } else {
            return "ERRORE comando sconosciuto";
        }
    }
}
