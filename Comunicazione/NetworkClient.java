package Comunicazione;

import java.io.*;
import java.net.*;

/* 
 Classe per la gestione della comunicazione di rete del client.
 Invio le richiesye tramite socket e ricevo le risposte dal server.
 Ho usato le classi PrintWriter e BufferedReader per poter leggere e scrivere per righe invece che per carattere.
 */

public class NetworkClient {

    public static String sendRequest(String ip, int porta, String message) {
        
        String Risposta = null;

        try(

            /* 
            Uso PrintWriter e BufferedReader per inviare e ricevere messaggi dal server

            Come visto nelle slide BufferedReader mi consete di leggere messaggi per righe, 
            invece che per carattere come InputStraemReader.

            Contestualmente PrintWriter mi permette di scrivere per righe
            invece che per carattere come OutputStreamWriter.

            */

            Socket socket = new Socket(ip, porta);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true); 
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Nel PrintWriter, true serve a svuotare il buffer dopo ogni invio

            // mettendo le risorse dentro il try vengono chiuse in automatico alla fine del blocco try, 
            // anche in caso di eccezione 
            // (try-with-resources è come se ci fosse un blocco finally che chiude le risorse con close)

        ){

            //imposto un timeout di 10 secondi entro il quale mi blocco se non ricevo risposta
            // in questo modo sono sicuro che se un nodo sensore accetta la conensione ma poi non risoponde, 
            // sto bloccato solo 10 sec.

            socket.setSoTimeout(10000);
            
            /* 
            ora invio i messaggi dal client al server con il PrintWriter 
            e leggo la risposta dal server con il BufferedReader, 
            Salvo la risposta del server nella variabile Risposta, che poi ritornerò al chiamante.
            */ 

            // Invia il messaggio al server
            out.println(message);

            //message sarà una stringa elaborata dal GestoreMessaggi con comando e parametri

            // Leggi la risposta dal server
            Risposta = in.readLine();


        } catch (IOException e) {
        }

    
        return Risposta;
    }
}
