package Sensore;

import java.io.*;
import java.net.*;

public class PeerServer implements Runnable {

    private final int portaP2P;
    private final ArchivioLocale archivio;
    private volatile boolean inEsecuzione;

    // Costruttore
    public PeerServer(int portaP2P, ArchivioLocale archivio) {

        this.portaP2P = portaP2P;
        this.archivio = archivio;
        this.inEsecuzione = true;

    }


    // Il metodo run avvia il server P2P che gestisce le richieste.
    // Come per l'aggregatore in listenForClients, usa un ciclo while che accetta con il metodo del serverSocket accept.
    // In questo caso non creo nuovi thread ma faccio gestire tutte le richieste ad uno, così gestisco una richiesta alla volta
    
    @Override
    public void run() {

        try (ServerSocket serverSocket = new ServerSocket(portaP2P)) {

            System.out.println("[SERVER P2P] In ascolto sulla porta P2P " + portaP2P);

            while (inEsecuzione) {

                try {
                    // Resta in attesa di richieste di connessione da parte del Server socket
                    Socket clientSocket = serverSocket.accept();
                    
                    // Invia il file richiesto in modo mutualmente esclusivo
                    sendFile(clientSocket);

                } catch (IOException e) {

                    if (!inEsecuzione) {
                        break;
                    }

                    System.err.println("[SERVER P2P ERRORE] Errore nell'accettare la connessione: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[SERVER P2P ERRORE] Impossibile avviare il ServerSocket sulla porta "  + portaP2P + ": " + e.getMessage());
        }
    }



    // sendFile opera in modo mutualmente esclusivo su un solo thread in modo che io possa servire una sola richiesta alla volta
    public synchronized void sendFile(Socket socket) {

        // Istanzia l'handler per processare la richiesta
        PeerRequestHandler handler = new PeerRequestHandler(socket, archivio);
        
        // Invocazione diretta di run() nello stesso thread (senza start()) 
        // per mantenere bloccata la sezione critica fino al completamento dell'invio.
        handler.run();
    }

    // Quando viene fatto un quit viene fermato anche il thread del peerServer
    public void stopServer() {
        this.inEsecuzione = false;
    }
}