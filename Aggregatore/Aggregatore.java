package Aggregatore;

import java.io.*;
import java.net.*;
import java.util.*;


public class Aggregatore {

    private static int porta;
    private static ServerSocket serverSocket;
    private static final Registro registro = new Registro();
    private static final Logger logger = new Logger();
    private static volatile boolean inEsecuzione = true;



    //Aggiungere commento metodo

    public static void main(String[] args) {

        if (args.length < 1) {

            System.err.println("Uso: java aggregatore.Aggregatore <porta>");
            System.exit(1);

        }

        try {

            porta = Integer.parseInt(args[0]);

        } catch (NumberFormatException e) {

            System.err.println("Errore: la porta deve essere un numero intero valido.");
            System.exit(1);

        }

        System.out.println("Avvio del Nodo Aggregatore sulla porta " + porta + "...");

        // Avvio del thread per l'interfaccia CLI da tastiera[cite: 1, 2]
        Thread threadCLI = new Thread(Aggregatore::startCLI);
        threadCLI.start();

        // Avvio del loop di ascolto per le connessioni dei client sensori[cite: 1, 2]
        listenForClients();
    }


    // Aggiungere commento metodo

    private static void startCLI() {

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("CLI dell'Aggregatore attiva. Comandi disponibili: listdata, log, quit");

        while (inEsecuzione) {

            try {

                System.out.print("> ");
                String linea = reader.readLine();

                if (linea == null) {

                    break;

                }

                String comando = linea.trim();

                if (comando.equals("listdata")) {

                    System.out.println("Risorse registrate sulla rete:");
                    // Visualizza lo stato aggiornato del registro globale[cite: 2, 4]
                    System.out.println(registro.toString());

                } else if (comando.equals("log")) {

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

                    System.out.println("Arresto del nodo Aggregatore...");
                    inEsecuzione = false;

                    if (serverSocket != null && !serverSocket.isClosed()) {

                        try {
                            serverSocket.close();

                        } catch (IOException e) {

                            System.err.println("Errore durante la chiusura della socket: " + e.getMessage());

                        }
                    }

                    System.exit(0);

                } else if (!comando.isEmpty()) {

                    System.out.println("Comando non riconosciuto. Comandi validi: listdata, log, quit");

                }

            } catch (IOException e) {

                if (inEsecuzione) {

                    System.err.println("Errore durante la lettura da tastiera");

                }
            }
        }
    }

    

    //Aggiungere commento metodo

    private static void listenForClients() {

        try {

            serverSocket = new ServerSocket(porta);
            System.out.println("Aggregatore in ascolto sulla porta TCP " + porta + "...");

            while (inEsecuzione) {

                Socket clientSocket = serverSocket.accept();
                // Istanzia ed avvia un thread GestoreClient per ogni richiesta accetta[cite: 1, 2]
                GestoreClient gestore = new GestoreClient(clientSocket, registro, logger);
                new Thread(gestore).start();

            }

        } catch (IOException e) {

            if (inEsecuzione) {

                System.err.println("Socket di ascolto chiusa o errore di rete: " + e.getMessage());
            }

        } finally {

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
