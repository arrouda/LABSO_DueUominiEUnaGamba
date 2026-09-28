package Aggregatore;

import java.io.*;
import java.net.*;
import java.util.*;

/* La classe Aggregatore contiene la logica principale dell'aggregatore, 
gestisce i thread per la cli e avvia il thread dedicato alla gestione dei client, sempre in ascolto per le richieste */

public class Aggregatore {

    private static int porta;
    private static ServerSocket serverSocket;
    private static final Registro registro = new Registro();
    private static final Logger logger = new Logger();
    private static volatile boolean inEsecuzione = true;


    public static void main(String[] args) {

        // mi assicuro che ci siano il numero di elementi necessari per avere la chiamata corretta con la porta su cui stare in ascolto
        if (args.length < 1) {

            System.err.println("Uso: java aggregatore.Aggregatore <porta>");
            System.exit(1);

        }

        // Se c'è almeno un argomento verifico che la porta inserita sia un numero valido
        try {

            porta = Integer.parseInt(args[0]);

        } catch (NumberFormatException e) {

            System.err.println("Errore: la porta deve essere un numero intero valido.");
            System.exit(1);

        }

        System.out.println("Avvio del Nodo Aggregatore sulla porta " + porta + "...");

        // Creo un nuovo thread che si occupi di eseguire parallelamente startCLI
        Thread threadCLI = new Thread(Aggregatore::startCLI); 
        threadCLI.start(); //avvio del thread

        // Avvio del loop di ascolto per le connessioni dei client sensori in un nuovo thread
        listenForClients();
    }




    // Start CLI è un metodo che opera in un thread dedicato e gestisce le interazionii da terminale dell'utenete
    // uso il buffer reader 

    private static void startCLI() {

        // TESTARE E DECIDERE SE TENERE LO SCANNER O IL BUFFER READER

        // IMPLEMENTAZIONE CON SCANNER

        Scanner scanner = new Scanner(System.in);
        System.out.println("CLI dell'Aggregatore attiva. Comandi disponibili: listdata, log, quit");

        while (inEsecuzione) {

            System.out.print("> ");
        
            // Verifica se c'è una riga da leggere
            if (!scanner.hasNextLine()) {
                break;
            }

            String linea = scanner.nextLine();
            String comando = linea.trim();

            if (comando.equals("listdata")) {

                // Listdata richiama il comando registro.toString che restituisce tutte le informazioni del registro come stringa
                // (vedere comando specifico nella classe registro)

                System.out.println("Risorse:");
                System.out.println(registro.toString());

                
            } else if (comando.equals("log")) {

                // il log chiama il comando di logger getLogs che restituisce la lista di log
                // A questo punto faccio un for-each su tutti per stampare i logs

                System.out.println("Risorse scaricate:");
                List<String> logs = logger.getLogs();

                if (logs.isEmpty()) {

                    System.out.println("(Nessun download effettuato)");

                } else {

                    for (String logEntry : logs) {
                        System.out.println("- " + logEntry);
                    }
                }


            } else if (comando.equals("quit")) {

                // comando di arresto del nodo che chiude il serverSocket 
                // e modifica la variabile booleana inEsecuzione per uscire dal ciclo

                System.out.println("Arresto del nodo Aggregatore...");
                inEsecuzione = false;

                if (serverSocket != null && !serverSocket.isClosed()) {

                    try {

                        serverSocket.close();
                        scanner.close();

                    } catch (IOException e) {

                        System.err.println("Errore durante la chiusura della socket: " + e.getMessage());

                    }
                }

                System.exit(0);

            
            } else if (!comando.isEmpty()) {
                
                // se il comando esiste ma non è tra qelli che conosco genero un errore
                System.err.println("Comando non riconosciuto. Comandi validi: listdata, log, quit");

            }
        }
    }



    // listeForClients crea un server socket in ascolto, accetta le richieste con accept()
    // e avvia un nuovo thread che gestisca le richieste.

    // il thread muore alla fine della gestione della richiesta.

    private static void listenForClients() {

        try {

            serverSocket = new ServerSocket(porta);
            System.out.println("Aggregatore in ascolto sulla porta TCP " + porta + "...");

            /* ciclo while che continua finchè non avviene un quit.
            Il serverSocket accetta le richieste che arrivano dai client e crea nuovi thread a cui delega la gestione della richiesta.
            Delegando la richiesta si libera subito il thread che rincomincia e si mette disponibile ad accettare una nuova richiesta. */

            while (inEsecuzione) {

                Socket clientSocket = serverSocket.accept();

                // l'aggregatore istanzia ed avvia un thread GestoreClient per ogni richiesta accetta
                GestoreClient gestore = new GestoreClient(clientSocket, registro, logger);
                new Thread(gestore).start();

            }

        } catch (IOException e) {

            if (inEsecuzione) {
                System.err.println("Socket di ascolto chiusa o errore di rete: " + e.getMessage());
            }
        
        } finally {

            // una volta terminato chiudo il serverSocket
            if (serverSocket != null && !serverSocket.isClosed()) {

                try {

                    serverSocket.close();

                } catch (IOException e) {

                    System.err.println("Errore nella chiusura finale della socket: " + e.getMessage());
                    
                }
            }
        }
    }
}
