package Comunicazione;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/* 
ntowrkClient è una classe che permette ai client di intergaire con i server inviando domande.
sfrutto printwriter con outputStreamWriter in modo da poter scrivere per riga invece che per singoli 
*/


public class NetworkClient {

    public static String sendRequest(String ip, int porta, String message) {

        String risposta = null;

        // Istanzio il socket vuoto per poter applicare il timeout sia alla connessione che alla lettura

        try (Socket socket = new Socket()) {

            // Imposta il timeout di connessione a 10 secondi (10000 ms)
            // il socket si connette 
            socket.connect(new InetSocketAddress(ip, porta), 10000);

            // Imposta il timeout di lettura (SO_TIMEOUT) a 10 secondi
            socket.setSoTimeout(10000);

            // Con Printwriter e BufferReader creo i "canali" di scrittura e lettura sul socket
            // Come detto scrivo per righe invece che per singoli caratteri.

            // È fondamentale mettere l'autoFLush con true dentro il printWriter 
            // perchè così il buffer si svuota e i messaggi vengono inviati subito

            try (
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                // Invia il messaggio formattato
                out.println(message);

                // Dopo aver inviato la richiesta i dati vengono trasmessi in rete
                // A questo punto uso il comando in.readLine che mette il sistema di risposta e appena la riceve la trascrive
                // Se però la risposta non arriva entro il timer fissato con setSoTimeout scatta un eccezione

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