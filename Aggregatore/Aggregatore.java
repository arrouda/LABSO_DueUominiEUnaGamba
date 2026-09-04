import java.io.*;
import java.net.*;

public class Aggregatore {

    public static void main(String[] args) {
        //legge la porta da riga di comando; inizializza le risorse condivise
        // e avvia il loop di ascolto sul ServerSocket e il thread della CLI
    }

    private static void startCLI(){
        //gestisce un loop per i comanddi locali da tastiera (listdata, log, quit)
    }

    private static void listenForClients(){
        //loop continuo con serverSocket.acceot() per accettare nuove connessioni
        // dai sensori e instanziare per ciascuna un ClientHandler su un nuovo thread
    }

}