package sensore;

import java.io.*;
import java.net.*;

public class PeerServer implements Runnable {

    private final int portaP2P;
    private final ArchivioLocale archivio;
    private volatile boolean inEsecuzione;

    public PeerServer(int portaP2P, ArchivioLocale archivio) {
        this.portaP2P = portaP2P;
        this.archivio = archivio;
        this.inEsecuzione = true;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(portaP2P)) {
            System.out.println("[SERVER P2P] In ascolto sulla porta P2P " + portaP2P);

            while (inEsecuzione) {
                try {
                    // Resta in attesa di connessioni P2P in ingresso
                    Socket clientSocket = serverSocket.accept();
                    
                    // Invia il file richiesto in modo mutualmente esclusivo
                    sendFile(clientSocket);
                } catch (IOException e) {
                    if (!inEsecuzione) {
                        break; // Server fermato intenzionalmente
                    }
                    System.err.println("[SERVER P2P ERRORE] Errore nell'accettare la connessione: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER P2P ERRORE] Impossibile avviare il ServerSocket sulla porta " 
                               + portaP2P + ": " + e.getMessage());
        }
    }

    public synchronized void sendFile(Socket socket) {
        // Istanzia l'handler per processare la richiesta
        PeerRequestHandler handler = new PeerRequestHandler(socket, archivio);
        
        // Invocazione diretta di run() nello stesso thread (senza start()) 
        // per mantenere bloccata la sezione critica fino al completamento dell'invio
        handler.run();
    }

    public void stopServer() {
        this.inEsecuzione = false;
    }
}
