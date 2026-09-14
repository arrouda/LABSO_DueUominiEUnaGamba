package sensore;

import java.util.*;
import java.io.*;
import java.net.*;

public class PeerRequestHandler implements Runnable {
    
    //Creo il socket di comunicazione con il nodo richiedente
    private Socket clientSocket;

    //Creo il riferimento all'archivio locale condiviso da cui leggere i dati
    private ArchivioLocale archivio;

    //Creo il costruttore
    public PeerrequestHandler(Socket clientSocket, ArchivioLocale archivio){
        this.clientSocket = clientSocket;
        this.archivio = archivio;
    }



     //Gestisce l'interazioen diretta di invio del file sulla singola socket verso il sensore che ha richiesto il download
    @Override
    public void run() {
       
    }

}
