package sensore;

import java.util.*;
import java.io.*;
import java.net.*;

public class PeerServer implements Runnable {

    private int porta;

    private ArchivioLocale archivio;

    private ServerSocket serverSocket;

    private volatile boolean inEsecuzione;

    public PeerServer(int porta, ArchivioLocale archivio){
        this.porta = porta;
        this.archivio = archivio;
        this.inEsecuzione = true;
    }

    //Ciclo principale del server peer. Accetta le connessioni in arrivo ed esegue l'handler.
    @Override
    public void run(){
        try{
            
            this.serverSocket = new ServerSocket(this.porta);
            System.out.println("In ascolto per download P2P sulla porta: " + this.porta);

            while(this.inEsecuzione){
                try{
                    
                    Socket socketClient = this.serverSocket.accept();
                    sendFile(socketClient);

                } catch(IOException e){
                    if(!this.inEsecuzione){
                        break;
                    }
                    System.out.println("Errore nell'accettare connessione peer");
                }
            }

        }catch(IOException e){
            System.out.println("Impossibile avviare il server sulla porta " + this.porta);
        }finally{
            arresta();
        }
    }

    //Gestisco il trasferimento del file verso il socket connesso
    public synchronized void sendFile(Socket socketClient) {
        PeerRequestHandler handler = new PeerRequestHandler(socketClient, this.archivio);
        handler.run();
    }

    //Creo il metodo per arrestare il server quando il nodo si disconnette.
    public void arresta(){
        this.inEsecuzione = false;
        try{
            if(this.serverSocket != null && !this.serverSocket.isClosed()){
                this.serverSocket.close();
            }
        }catch(IOException e){
            System.out.println("Errore durante la chiusura del socket server");
        }
    }

    //Ritorna la porta effettiva su cui il server e' in ascolto.
    public int getPorta(){
        return this.porta;
    }

}