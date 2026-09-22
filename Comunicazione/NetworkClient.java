package Comunicazione;

import java.io.*;
import java.net.*;

/**
 * Classe per la gestione della comunicazione di rete TCP del client.
 * Invia richieste tramite socket "usa-e-getta" e riceve le risposte dal server/peer.
 * Utilizza PrintWriter e BufferedReader per la lettura/scrittura a righe di testo.
 */
public class NetworkClient {

    public static String sendRequest(String ip, int porta, String message) {
        String risposta = null;

        // Istanzia il socket vuoto per poter applicare il timeout sia alla connessione che alla lettura
        try (Socket socket = new Socket()) {

            // Imposta il timeout di connessione a 10 secondi (10000 ms)
            socket.connect(new InetSocketAddress(ip, porta), 10000);

            // Imposta il timeout di lettura (SO_TIMEOUT) a 10 secondi
            socket.setSoTimeout(10000);

            // Inizializza i buffer di I/O legati alla socket
            try (
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                // Invia il messaggio formattato
                out.println(message);

                // Legge la riga di risposta inviata dal remoto
                risposta = in.readLine();
            }

        } catch (IOException e) {
            // In caso di errore di connessione, timeout o nodo irraggiungibile,
            // ritorna null consentendo al chiamante (es. RobustDownloader) di gestire il fallimento.
            risposta = null;
        }

        return risposta;
    }
}