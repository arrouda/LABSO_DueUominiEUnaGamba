package Sensore;

import java.util.*;
import java.io.*;
import java.net.*;

import Comunicazione.*;

public class PeerRequestHandler implements Runnable {
    
    //Creo il socket di comunicazione con il nodo richiedente
    private Socket clientSocket;

    //Creo il riferimento all'archivio locale condiviso da cui leggere i dati
    private ArchivioLocale archivio;

    //Creo il costruttore
    public PeerRequestHandler(Socket clientSocket, ArchivioLocale archivio){
        this.clientSocket = clientSocket;
        this.archivio = archivio;
    }

    //Gestisce l'interazioen diretta di invio del file sulla singola socket verso il sensore che ha richiesto il download
    @Override
    public void run() {
        
        try{
            
            //Canale per leggere la richiesta dal peer
            InputStreamReader is = new InputStreamReader(this.clientSocket.getInputStream());
            BufferedReader lettore = new BufferedReader(is);
            
            //Canale per inviare la risposta al peer
            PrintWriter scrittore = new PrintWriter(this.clientSocket.getOutputStream(), true);

            //Leggo la riga inviata dal peer
            String messaggioRicevuto = lettore.readLine();

            if(messaggioRicevuto != null && !messaggioRicevuto.isEmpty()){
                //Decodifico il messaggio usando il metodo del gestore comune
                Map<String, String> parametri = GestoreMessaggi.parseMessage(messaggioRicevuto);
                String comando = parametri.get("comando");
                //Verifico che il comando sia effettivamente un download
                if("DOWNLOAD".equals(comando)){
                    String nomeMisura = parametri.get("misurazione");
                    //Recupero il contenuto dell'archivio locale
                    String contenuto = this.archivio.getContent(nomeMisura);
                    //Invio il valore se trovato, senno' invio stringa vuota
                    if(contenuto != null){
                        scrittore.println(contenuto);
                    }else{
                        //Inviando una riga vuota, NetworkClient e RobustDownloader riconoscono che il download e' finito
                        scrittore.println(""); 
                    }
                }
            }

            //Chiudo i canali e il socket per liberare le risorse 
            lettore.close();
            scrittore.close();
            this.clientSocket.close();

        }catch(IOException e){
            System.out.println("Errore nel servire le richieste del peer");
        }

    }

}
