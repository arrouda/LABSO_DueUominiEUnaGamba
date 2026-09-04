import java.net.Socket;

public class PeerServer implements Runnable {

    @Override
    public void run() {
        // mantiene attivo un server socket per il trasferimento peer to peer

        // TO DO
    }

    synchronized void sendFile(Socket socket, String Nome) {
        // invia rilevazione richiesta via socket garantendo di servire una sola
        // richiesta
        // accodo le altre attraverso la sincronizzazione

        // TO DO
    }

}
