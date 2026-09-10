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
    public static void main(String[] args) throws IOException {
        int porta = Integer.parseInt(args[0]);
 
        Aggregatore aggregatore = new Aggregatore();
        aggregatore.registro = new Registro();
        aggregatore.log = new Logger();
        aggregatore.socketServer = new ServerSocket(porta);
 
        // accept() blocca il thread che lo chiama finché non arriva qualcuno,
        // quindi lo mettiamo su un thread a parte: così il thread principale
        // resta libero per leggere i comandi che l'utente digita da tastiera
        Thread threadAscolto = new Thread(() -> aggregatore.listenForClients());
        threadAscolto.start();
 
        aggregatore.startCLI();
    }

    // ciclo dei comandi digitati a tastiera mentre l'aggregatore è in esecuzione

    private void startCLI() throws IOException {
        //gestisce un loop per i comanddi locali da tastiera (listdata, log, quit)
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
 
    // resta in ascolto di nuove connessioni: ogni volta che un sensore si
    // collega, viene creato un GestoreClient dedicato su un thread nuovo,
    // così più sensori possono essere serviti contemporaneamente
    


    private void listenForClients(){
        //loop continuo con serverSocket.acceot() per accettare nuove connessioni
        // dai sensori e instanziare per ciascuna un ClientHandler su un nuovo thread

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