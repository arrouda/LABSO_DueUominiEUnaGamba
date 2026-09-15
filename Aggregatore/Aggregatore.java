package Aggregatore;
 
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
 
public class Aggregatore {
 
    private Registro registro;
    private Logger log;
    private ServerSocket socketServer;
 
    // punto d'ingresso: ci si aspetta un solo argomento, la porta su cui
    // l'aggregatore deve mettersi in ascolto
    // Qui viene preparato tutto il necessario prima di accettare connessioni:
    // le due risorse condivise tra tutti i client (registro delle rilevazioni
    // e log delle operazioni) e il socket del server.
    //
    // Nota: le specifiche (pag. 10) usano "Master" come nome di esempio per
    // questa classe (java Master 9000); qui è rimasto "Aggregatore" per
    // coerenza col nome del package — da verificare con docente/tutor se il
    // nome conta ai fini della valutazione (vedi documento incongruenze,
    // punto 8).
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Aggregatore.Aggregatore <porta>");
            return;
        }
 
        int porta;
        try {
            porta = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("La porta deve essere un numero intero: " + args[0]);
            return;
        }
 
        Aggregatore aggregatore = new Aggregatore();
        aggregatore.registro = new Registro();
        aggregatore.log = new Logger();
 
        try {
            aggregatore.socketServer = new ServerSocket(porta);
        } catch (IOException e) {
            System.err.println("Impossibile aprire il server sulla porta " + porta + ": " + e.getMessage());
            return;
        }
 
        // accept() blocca il thread che lo chiama finché non arriva qualcuno,
        // quindi lo mettiamo su un thread a parte: così il thread principale
        // resta libero per leggere i comandi che l'utente digita da tastiera
        Thread threadAscolto = new Thread(() -> aggregatore.listenForClients());
        threadAscolto.start();
 
        try {
            aggregatore.startCLI();
        } catch (IOException e) {
            System.err.println("Errore nella CLI: " + e.getMessage());
        }
    }
 
    // ciclo dei comandi digitati a tastiera mentre l'aggregatore è in esecuzione
 
    private void startCLI() throws IOException {
        // gestisce un loop per i comandi locali da tastiera (listdata, log, quit)
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String comando = scanner.nextLine();
 
            if (comando.equals("listdata")) {
                // chiediamo al registro tutte le rilevazioni note e per ognuna
                // stampiamo anche chi la possiede, come richiesto dalle specifiche
                System.out.println("Risorse:");
                for (Map.Entry<String, List<String>> voce : registro.getAllData().entrySet()) {
                    System.out.println("  - " + voce.getKey() + ": " + String.join(", ", voce.getValue()));
                }
            } else if (comando.equals("log")) {
                // stampiamo semplicemente le voci così come le ha registrate il Logger
                for (String riga : log.getLogs()) {
                    System.out.println(riga);
                }
            } else if (comando.equals("quit")) {
                // chiudendo il server socket, la accept() in listenForClients()
                // lancia un'eccezione: è così che facciamo terminare anche quel thread
                socketServer.close();
                break;
            } else {
                System.out.println("Comando non riconosciuto");
            }
        }
    }
 
    // resta in ascolto di nuove connessioni: ogni volta che arriva qualcuno
    // (un sensore che fa REGISTER, oppure un RobustDownloader che chiede
    // REQUEST_TOKEN/RELEASE_TOKEN/NODE_FAILED con una connessione usa-e-getta)
    // viene creato un GestoreClient dedicato su un thread nuovo, così più
    // richieste possono essere servite contemporaneamente
 
    private void listenForClients() {
        // loop continuo con serverSocket.accept() per accettare nuove connessioni
        // e instanziare per ciascuna un GestoreClient su un nuovo thread
 
        while (true) {
            try {
                Socket socket = socketServer.accept();
                GestoreClient gestore = new GestoreClient(socket, registro, log);
                new Thread(gestore).start();
            } catch (IOException e) {
                // ci arriviamo quando il socket viene chiuso dal comando "quit":
                // non è un vero errore, è solo il segnale per uscire dal loop
                break;
            }
        }
    }
 
}
 