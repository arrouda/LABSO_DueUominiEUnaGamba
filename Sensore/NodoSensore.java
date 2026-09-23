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

        idNodo = "peer_" + portaP2P;

        // 1. Inizializzazione dell'Archivio Locale
        archivio = new ArchivioLocale();

        // 2. Avvio del Server P2P in background (Thread secondario per servire gli altri nodi)
        peerServer = new PeerServer(portaP2P, archivio);
        Thread threadPeerServer = new Thread(peerServer);
        threadPeerServer.setDaemon(true); // Thread daemon per chiudersi alla chiusura della CLI
        threadPeerServer.start();

        // 3. Inizializzazione del Downloader Robusto
        downloader = new RobustDownloader(ipAggregatore, portaAggregatore);

        System.out.println("=== NODO SENSORE AVVIATO ===");
        System.out.println("ID Nodo: " + idNodo + " | Porta P2P locale: " + portaP2P);
        System.out.println("Aggregatore: " + ipAggregatore + ":" + portaAggregatore);

        // 4. Registrazione iniziale presso l'Aggregatore centrale
        registrazioneAggregatore();

        // 5. Avvio della sessione interattiva CLI (Thread Main)
        startCLI();
    }

    
    public static void registrazioneAggregatore() {
        List<String> risorseLocali = archivio.getLocalData();
        String misurazioniStr = String.join(",", risorseLocali);

        Map<String, String> parametri = new HashMap<>();
        parametri.put("id", idNodo);
        parametri.put("ip", "127.0.0.1"); // Indirizzo IP del sensore
        parametri.put("porta", String.valueOf(portaP2P));
        // Per massima compatibilità con l'Aggregatore, inviamo sia 'misurazioni' che 'risorse'
        parametri.put("misurazioni", misurazioniStr);
        parametri.put("risorse", misurazioniStr);

        String messaggio = GestoreMessaggi.serializedMessage("REGISTER", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        if (risposta != null && !risposta.isEmpty()) {
            Map<String, String> mappaRisposta = GestoreMessaggi.parseMessage(risposta);
            String status = mappaRisposta.get("status");
            
            if ("OK".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(risposta.trim())) {
                System.out.println("[REGISTRAZIONE] Registrazione presso l'Aggregatore completata con successo.");
            } else {
                System.err.println("[ERRORE REGISTRAZIONE] L'Aggregatore ha risposto: " + risposta);
            }
        } else {
            System.err.println("[ERRORE CONNETTIVITÀ] Impossibile raggiungere l'Aggregatore a " 
                               + ipAggregatore + ":" + portaAggregatore);
        }
    }

    
    public static void disconnettiAggregatore() {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("id", idNodo);

        String messaggio = GestoreMessaggi.serializedMessage("QUIT", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
        System.out.println("[DISCONNESSIONE] Notifica di disconnessione inviata all'Aggregatore.");
    }

    
    public static void startCLI() {
        Scanner scanner = new Scanner(System.in);
        boolean inEsecuzione = true;

        System.out.println("\nComandi disponibili:");
        System.out.println(" - listdata local");
        System.out.println(" - listdata remote");
        System.out.println(" - add <nome risorsa> <contenuto>");
        System.out.println(" - download <nome risorsa>");
        System.out.println(" - quit");

        while (inEsecuzione) {
            System.out.print("\n> ");
            if (!scanner.hasNextLine()) break;

            String riga = scanner.nextLine().trim();
            if (riga.isEmpty()) continue;

            String[] parti = riga.split("\\s+", 3);
            String comando = parti[0].toLowerCase();

            switch (comando) {
                case "listdata":
                    if (parti.length >= 2 && parti[1].equalsIgnoreCase("local")) {
                        System.out.println("Risorse locali:");
                        List<String> locali = archivio.getLocalData();
                        if (locali.isEmpty()) {
                            System.out.println("(Nessuna misurazione presente in locale)");
                        } else {
                            for (String m : locali) {
                                System.out.println("- " + m);
                            }
                        }
                    } else if (parti.length >= 2 && parti[1].equalsIgnoreCase("remote")) {
                        richiediListDataRemota();
                    } else {
                        System.out.println("Sintassi errata. Usa: 'listdata local' oppure 'listdata remote'");
                    }
                    break;

                case "add":
                    if (parti.length >= 3) {
                        String nomeRisorsa = parti[1];
                        String contenuto = parti[2];
                        archivio.aggiungiMisura(nomeRisorsa, contenuto);
                        System.out.println("Misurazione '" + nomeRisorsa + "' aggiunta in locale.");
                        // Ri-notifica l'aggregatore dell'aggiornamento delle risorse
                        registrazioneAggregatore();
                    } else {
                        System.out.println("Sintassi errata. Usa: add <nome risorsa> <contenuto>");
                    }
                    break;

                case "download":
                    if (parti.length >= 2) {
                        String nomeRisorsa = parti[1];
                        System.out.println("Avvio ciclo di download robusto per '" + nomeRisorsa + "'...");
                        String risultato = downloader.downloadWhitRetry(nomeRisorsa, idNodo);
                        
                        if (risultato != null && !risultato.isEmpty()) {
                            archivio.aggiungiMisura(nomeRisorsa, risultato);
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
                    disconnettiAggregatore();
                    inEsecuzione = false;
                    System.out.println("Chiusura del Nodo Sensore.");
                    break;

                default:
                    System.out.println("Comando sconosciuto. Comandi disponibili: listdata local, listdata remote, add, download, quit");
                    break;
            }
        }
        scanner.close();
        System.exit(0);
    }

    
    private static void richiediListDataRemota() {
        Map<String, String> parametri = new HashMap<>();
        parametri.put("id", idNodo);

        String messaggio = GestoreMessaggi.serializedMessage("LISTDATA", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        if (risposta != null && !risposta.isEmpty()) {
            Map<String, String> mappa = GestoreMessaggi.parseMessage(risposta);
            System.out.println("Risorse distribuite sulla rete:");
            String listaStr = mappa.getOrDefault("risorse", mappa.getOrDefault("lista", risposta));
            
            if (listaStr != null && !listaStr.isEmpty()) {
                String[] elementi = listaStr.split(";");
                for (String elem : elementi) {
                    System.out.println("- " + elem);
                }
            } else {
                System.out.println("(Nessuna risorsa remota registrata)");
            }
        } else {
            System.err.println("Impossibile recuperare la lista remota dall'Aggregatore.");
        }
    }
}