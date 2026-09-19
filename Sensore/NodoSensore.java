package Sensore;

import java.util.*;
import java.io.*;
import java.net.*;

import Comunicazione.*;

//Questa e' la classe principale del nodo sensore e gestisce il ciclo di vita del client:
// Connessione e registrazione presso l'aggregatore
// Avvio del server P2P in background per servire download agli altri nodi
// Interfaccia a riga di comando (CLI) per l'interazione con l'utente
public class NodoSensore{
    
    private static String ipAggregatore;
    private static int portaAggregatore;
    private static String nodeId;
    private static int portaP2P = 9500;
    private static ArchivioLocale archivio;
    private static PeerServer peerServer;
    
    public static void main(String[] args) {
        
        if(args.length < 2){
            System.out.println("Uso scorretto");
            return;
        }

        ipAggregatore = args[0];

        try{
            
            portaAggregatore = Integer.parseInt(args[1]);

            if(args.length >= 3){
                portaP2P = Integer.parseInt(args[2]);
            }

        }catch(NumberFormatException e){
            System.out.println("Errore: la porta deve essere un numero intero.");
            return;
        }

        nodeId = "peer_" + portaP2P;
        
        archivio = new ArchivioLocale();
        
        peerServer = new PeerServer(portaP2P, archivio);
        Thread threadServer = new Thread(peerServer);
        threadServer.start();

        boolean registrato = registrazioneAggregatore();
        
        if(!registrato){
            System.out.println("Impossibile connettersi o registrarsi all'aggregatore su " + ipAggregatore + ":" + portaAggregatore);
            peerServer.arresta();
            return;
        }

        System.out.println("Sensore avviato con successo [ID: " + nodeId + "]. In attesa di comandi...");

        startCLI();

    }

    public static void startCLI() {
       
        Scanner scanner = new Scanner(System.in);
        
        while(true){
            
            System.out.print("> ");
            String linea = scanner.nextLine().trim();
            
            if(linea.isEmpty()){
                continue;
            }
            
            String[] parti = linea.split("\\s+");
            String comando = parti[0].toLowerCase();
            
            if(comando.equals("listdata")){
                
                if(parti.length > 1 && parti[1].equalsIgnoreCase("local")){
                    List<String> locali = archivio.getLocalData();
                    System.out.println("Risorse");
                    
                    for(String res : locali){
                        System.out.println("- " + res);
                    }

                }else if(parti.length > 1 && parti[1].equalsIgnoreCase("remote")){
                    richiediListDataRemote();
                }else{
                    System.out.println("Comando non valido");
                }

            }else if(comando.equals("add")){
                
                if(parti.length >= 3){
                   
                    String nomeRisorsa = parti[1];
                    StringBuilder sb = new StringBuilder();
                    
                    for(int i = 2; i < parti.length; i++){
                        sb.append(parti[i]).append(" ");
                    }

                    String contenuto = sb.toString().trim();
                    archivio.aggiungiMisura(nomeRisorsa, contenuto);
                    notificaAggiuntaAggregatore(nomeRisorsa);
                    System.out.println("Risorsa '" + nomeRisorsa + "' aggiunta con successo.");
                
                }else{
                    System.out.println("Uso non corretto");
                }

            }else if(comando.equals("quit")){
                
                System.out.println("Disconnessione dal sensore in corso");
                disconnettiAggregatore();
                peerServer.arresta();
                System.out.println("Sensore arrestato");
                break;

            }else if(comando.equals("download")){
                
                if(parti.length >= 2){
                    String nomeRisorsa = parti[1];
                    System.out.println("Avvio download per la risorsa: " + nomeRisorsa + "...");
                    RobustDownloader.downloadWhitRetry(nomeRisorsa);
                }else{
                    System.out.println("Comando sconosciuto: '" + comando + "'");
                }

            }

        }

        scanner.close();

    }

    //Registra il sensore presso l'aggregatore inviando il proprio ID, la porta P2P e l'elenco iniziale delle rilevazioni locali.
    public static boolean registrazioneAggregatore() {
        
        try{
            
            Map<String, String> parametri = new HashMap<>();
            parametri.put("nodeId", nodeId);
            parametri.put("portaP2P", String.valueOf(portaP2P));
            
            List<String> datiLocali = archivio.getLocalData();
            parametri.put("dati", String.join(",", datiLocali));
            
            String messaggio = GestoreMessaggi.serilizedMessage("REGISTER", parametri);
            String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
            return risposta != null && !risposta.trim().isEmpty();

        }catch(Exception e){
            return false;
        }

    }

    //Invia la richiesta di 'listdata remote' all'aggregatore e ne stampa l'output.
    private static void richiediListDataRemote(){
        
        Map<String, String> parametri = new HashMap<>();
        parametri.put("nodeId", nodeId);

        String messaggio = GestoreMessaggi.serilizedMessage("LISTDATA_REMOTE", parametri);
        String risposta = NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);

        if(risposta != null && !risposta.isEmpty()){
            System.out.println("Risorse:");
            System.out.println(risposta);
        }else{
            System.out.println("Nessuna risorsa disponibile o errore nella risposta dell'aggregatore.");
        }

    }

    //Notifica l'aggregatore quando viene aggiunta una nuova misurazione in locale.
    private static void notificaAggiuntaAggregatore(String nomeRisorsa){
        
        Map<String, String> parametri = new HashMap<>();
        parametri.put("nodeId", nodeId);
        parametri.put("misurazione", nomeRisorsa);

        String messaggio = GestoreMessaggi.serilizedMessage("ADD_DATA", parametri);
        NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
    
    }
    
    
    //Notifica l'aggregatore prima di chiudere il programma in modo pulito.
    public static void disconnettiAggregatore() {
       
        try{
            
            Map<String, String> parametri = new HashMap<>();
            parametri.put("nodeId", nodeId);

            String messaggio = GestoreMessaggi.serilizedMessage("DISCONNECT", parametri);
            NetworkClient.sendRequest(ipAggregatore, portaAggregatore, messaggio);
        
        }catch (Exception e){
            System.out.println("Errore durante la notifica di disconnessione: " + e.getMessage());
        }

    }

}