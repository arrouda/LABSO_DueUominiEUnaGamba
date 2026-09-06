import java.io.*;
import java.net.*;

public class GestoreClient implements Runnable {

   public GestoreClient(Socket clientsocket, Registro registro, Logger logger) {
      // salva i riferimenti al socket e alle risorse condivise

   }

   public void run() {
      // getsisce il cilco di vita della connessione socket con un sinoglo sensore
      // legge messaggi in ingresso li interpreta e invia le risposte.

      // TODO
   }

   private String gestoreRichiesta(String richiesta) {
      // decodifica le richiesta ricevuta (registrati, chiedi lista, richiedi token)
      // e invoca i relativi metodi del Registro o Logger

      // TODO
   }

}