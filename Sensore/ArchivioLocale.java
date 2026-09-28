package Sensore;

import java.io.PrintWriter;
import java.util.*;

/*

 Gestisce il magazzino locale delle rilevazioni possedute dal Nodo Sensore.
 Mantiene la mappa nomeRisorsa -> contenutoMisura ed offre accesso thread-safe 
 synchronized per prevenire race condition tra l'interfaccia utente (CLI) 
 e le richieste P2P in ingresso gestite dal PeerServer.

 */

public class ArchivioLocale {

    // Struttura dati per memorizzare le misurazioni locali (Nome -> Contenuto)
    private final Map<String, String> misurazioni;


    public ArchivioLocale() {
        this.misurazioni = new HashMap<>();
    }

    // Inizializzo con delle misurazioni casuali inventate da me che aggiungo all'elenco delle misurazioni 
    // e stampo nel file dedicato
    public void inizializzazione(PrintWriter pw){

        String misurazione0 = "Temperratura = 31.5C;Pressione = 800hPa";
        String misurazione1 = "CO2 = 450ppm;Umidita = 55%";

        this.misurazioni.put("R0", misurazione0);
        pw.println("R0,"+ misurazione0);

        this.misurazioni.put("R1", misurazione1);
        pw.println("R1,"+misurazione1);

        pw.flush();

    }

    // Aggiungo una misura
    public synchronized void aggiungiMisura(String nome, String contenuto) {

        if (nome != null && !nome.trim().isEmpty() && contenuto != null) {
            this.misurazioni.put(nome.trim(), contenuto);
        }
    }


    // Recupero tutti i dati locali
    public synchronized List<String> getLocalData() {
        return new ArrayList<>(this.misurazioni.keySet());
    }

    // Recupero una misurazione specifica in base al nome
    public synchronized String getContent(String nome) {

        if (nome == null) {
            return null;
        }
        
        return this.misurazioni.get(nome.trim());
    }
}
