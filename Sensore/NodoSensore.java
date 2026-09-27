package Sensore;

import Comunicazione.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class NodoSensore {

    private static String ipAggregatore;
    private static int portaAggregatore;
    private static String idNodo;
    private static int portaP2P;

    private static ArchivioLocale archivio;
    private static RobustDownloader downloader;
    private static PeerServer peerServer;

    public static void main(String[] args) {

        // Controllo argomenti da riga di comando (java Client <IP_Aggregatore> <Porta_Aggregatore> [Porta_P2P])
        if (args.length < 2) {
            System.err.println("Uso: java Sensore.NodoSensore <IP_Aggregatore> <Porta_Aggregatore> [Porta_P2P]");
            System.exit(1);
        }

        ipAggregatore = args[0];
        portaAggregatore = Integer.parseInt(args[1]);

        // Configurazione della porta P2P locale (di default o assegnata)
        if (args.length >= 3) {
            portaP2P = Integer.parseInt(args[2]);
        } else {
            portaP2P = 9500 + (int) (Math.random() * 1000);
        }

        // Per dare l'id in modo univoco uso la porta (un solo nodo avrà una sola porta)
        idNodo = "peer_" + portaP2P;

        try{
            // Il pw creerà un file con il nome del nodo in cui salva lo storico delle rilevazioni
        
            PrintWriter pw = new PrintWriter(idNodo + ".txt");

            // Inizializzazione dell'Archivio Locale
            archivio = new ArchivioLocale();
            archivio.inizializzazione(pw);

            // Avvio del Server P2P in background (Thread secondario per servire gli altri nodi)
            peerServer = new PeerServer(portaP2P, archivio);
            Thread threadPeerServer = new Thread(peerServer);
            threadPeerServer.setDaemon(true); // Uso il Thread daemon per chiudersi alla chiusura della CLI
            threadPeerServer.start();

            // Inizializzazione del Downloader Robusto che permette al nodo di chiedere un token all'aggregatore 
            // e tentare il download da altri nodi
            downloader = new RobustDownloader(ipAggregatore, portaAggregatore);

            System.out.println("NODO SENSORE AVVIATO");
            System.out.println("ID Nodo: " + idNodo + " | Porta P2P locale: " + portaP2P);
            System.out.println("Aggregatore: " + ipAggregatore + ":" + portaAggregatore);

            // Registrazione iniziale presso l'Aggregatore centrale
            if (!registrazioneAggregatore()) {
                System.exit(1);
            }

            // Avvio della CLI sul trhead principale dell'aggregatore

            startCLI(pw);

            pw.close();

        }catch(IOException e){

            System.out.println("errore");
        }
        
    }

    

    // Metodo per registrarsi nell'aggregatore.
    // In questo metodo non uso l'indirizzo ip locale ma in modo dinamico recupero l'ip dall'host e comunico quello al programma

    public static boolean registrazioneAggregatore() {

        String ipLocale;

        List<String> risorseLocali = archivio.getLocalData();
        String misurazioniStr = String.join(",", risorseLocali);

        Map<String, String> parametri = new HashMap<>();

        // Recupero l'indirizzo ip con i metodi di rete di java
        try {
                
            ipLocale = InetAddress.getLocalHost().getHostAddress();

        } catch (UnknownHostException e) {

            // In caso di errore o assenza di risoluzione del nome macchina, si usa il fallback
            System.err.println("errore nel recuperare l'Ip ");
            ipLocale = "127.0.0.1";
        }

        // Inserisco i parametri per la registrazione
        parametri.put("id", idNodo);
        parametri.put("ip", ipLocale); // Indirizzo IP del sensore
        parametri.put("porta", String.valueOf(portaP2P));
        parametri.put("misurazioni", misurazioniStr);

        // Trasformo la mappa dei parametri per la registrazione in stringa con il comando di gestore messaggi e invio la richiesta
        String messaggio = GestoreMessaggi.serializedMessage("REGISTER", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        // Elaboro e stampo l'esito della registrazione
        if (risposta != null && !risposta.isEmpty()) {

            Map<String, String> mappaRisposta = GestoreMessaggi.parseMessage(risposta);
            String status = mappaRisposta.get("status");
            
            if ("OK".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(risposta.trim())) {

                System.out.println("[REGISTRAZIONE] Registrazione presso l'Aggregatore completata con successo.");
                return true;

            } else {

                System.err.println("[ERRORE REGISTRAZIONE] L'Aggregatore ha risposto: " + risposta);
                return false;

            }

        } else {

            System.err.println("[ERRORE CONNETTIVITÀ] Impossibile raggiungere l'Aggregatore a "  + ipAggregatore + ":" + portaAggregatore);
            return false;

        }
    }

    

    // Invoco il comando quit che rimuove il nodo dall'aggregatore
    public static void disconnettiAggregatore() {

        Map<String, String> parametri = new HashMap<>();
        parametri.put("id", idNodo);

        String messaggio = GestoreMessaggi.serializedMessage("QUIT", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
        System.out.println("[DISCONNESSIONE] Notifica di disconnessione inviata all'Aggregatore.");
    }



    // Questo è il metodo per la gestione della CLI ed è molto simile al metodo dell'aggrgeatore

    public static void startCLI(PrintWriter pw) {

        Scanner scanner = new Scanner(System.in);
        boolean inEsecuzione = true;

        // Stampo la lista di comandi disponibili

        System.out.println("\nComandi disponibili:");
        System.out.println(" - listdata local");
        System.out.println(" - listdata remote");
        System.out.println(" - add <nome risorsa> <contenuto>");
        System.out.println(" - download <nome risorsa>");
        System.out.println(" - quit");

        // Ciclo perenne per poter inserire i comandi

        while (inEsecuzione) {

            System.out.println();

            // Leggo ed elaboro il comando inviato
            // Prendo la stringa e la divido perchè nella cli del sensore posso avere anche dei parametri oltre che i comandi
            // Su questo è diverso dall'aggregatore perchè lì ho comandi senza parametri

            if (!scanner.hasNextLine()){
                break;
            }

            String riga = scanner.nextLine().trim();

            if (riga.isEmpty()){
                continue;
            }

            String[] parti = riga.split("\\s+", 3);
            String comando = parti[0].toLowerCase(); // il primo deve essere il comando

            switch (comando) {

                case "listdata":

                    // Con il comando listData devo gestire sia il caso di list data local che il caso di list data remote
                    if (parti.length >= 2 && parti[1].equalsIgnoreCase("local")) {

                        // Con il local stampo i dati locali presi dall'ArchivioLocale
                        System.out.println("Risorse:");
                        List<String> locali = archivio.getLocalData();

                        if (locali.isEmpty()) {

                            System.out.println("(Nessuna misurazione presente in locale)");

                        } else {

                            for (String m : locali) {
                                System.out.println("- " + m);
                            }
                        }

                    } else if (parti.length >= 2 && parti[1].equalsIgnoreCase("remote")) {

                        // Devo fare una richiesta all'aggregatore
                        richiediListDataRemota();

                    } else {

                        System.out.println("Sintassi errata. Usa: 'listdata local' oppure 'listdata remote'");
                    }

                    break;

                case "add":

                    // Questo è il metodo per l'aggiunta di una misurazione
                    // Quando ricevo una misurazione la devo aggiungere e poi comunicare all'aggregatore l'aggiunta

                    if (parti.length >= 3) {

                        String nomeRisorsa = parti[1];
                        String contenuto = parti[2];

                        archivio.aggiungiMisura(nomeRisorsa, contenuto);

                        // Aggiungo la risorsa allo storico
                        pw.println(nomeRisorsa + ", " + contenuto);
                        pw.flush(); // Per aggiunere subito la risorsa allo storico

                        System.out.println("Misurazione '" + nomeRisorsa + "' aggiunta in locale.");
                        // Ri-notifica l'aggregatore dell'aggiornamento delle risorse
                        registrazioneAggregatore();

                    } else {

                        System.out.println("Sintassi errata. Usa: add <nome risorsa> <contenuto>");

                    }

                    break;

                case "download":

                    // Con il download avvio il download da un altro nodo sensore
                    // Sfrutto il robustDownloader delle classi di comunicazione per scaricare la risorsa

                    if (parti.length >= 2) {

                        String nomeRisorsa = parti[1];
                        System.out.println("Avvio ciclo di download robusto per '" + nomeRisorsa + "'...");
                        String risultato = downloader.downloadWithRetry(nomeRisorsa, idNodo);
                        
                        // Se il download va a buon fine aggiunfo il risultato all'archivio, al registro dell'aggregatore e allo storico txt
                        if (risultato != null && !risultato.isEmpty()) {

                            archivio.aggiungiMisura(nomeRisorsa, risultato);

                            pw.println(nomeRisorsa + ", " + risultato);
                            pw.flush();

                            System.out.println("Download completato e salvato in locale!");
                            System.out.println("Contenuto: " + risultato);
                            // Notifica l'aggregatore del nuovo dato acquisito
                            registrazioneAggregatore();

                        } else {

                            System.err.println("Impossibile scaricare la risorsa '" + nomeRisorsa + "' dalla rete.");

                        }

                    } else {

                        System.out.println("Sintassi errata. Usa: download <nome risorsa>");

                    }

                    break;

                case "quit":

                    // Nel caso del quit mi scollego dall'aggregatore, fermo il thread del server e cambio inEsecuzione per uscire dal ciclo

                    disconnettiAggregatore();
                    inEsecuzione = false;
                    peerServer.stopServer();

                    System.out.println("Chiusura del Nodo Sensore.");

                    break;

                default: // Gestisco il caso del comando sconosciuto
                    System.out.println("Comando sconosciuto. Comandi disponibili: listdata local, listdata remote, add, download, quit");
                    break;
            }
        }

        scanner.close();
        System.exit(0);

    }

    
    // Metodo che viene richiamato in listdta Remote e chiede all'aggregatore ka kusta di tutte le rilevazioni salvate dagli altri nodi

    private static void richiediListDataRemota() {

        Map<String, String> parametri = new HashMap<>();

        parametri.put("id", idNodo);

        String messaggio = GestoreMessaggi.serializedMessage("LISTDATA", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        // Stampo la lista dei dati
        if (risposta != null && !risposta.isEmpty()) {

            Map<String, String> mappa = GestoreMessaggi.parseMessage(risposta);
            System.out.println("Risorse:");
            String listaStr = mappa.getOrDefault("risorse", mappa.getOrDefault("registro", risposta));
            
            if (listaStr != null && !listaStr.isEmpty()) {

                System.out.println(listaStr.trim());
                
            } else {

                System.out.println("(Nessuna risorsa remota registrata)");

            }
        } else {

            System.err.println("Impossibile recuperare la lista remota dall'Aggregatore.");
            
        }
    }
}