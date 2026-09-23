package Sensore;

import java.util.*;

 // Questa classe:
 // - Gestisce il magazzino locale delle rilevazioni possedute dal Nodo Sensore.
 // - Mantiene la mappa nomeRisorsa -> contenutoMisura ed offre accesso thread-safe 
 //   synchronized per prevenire race condition tra l'interfaccia utente (CLI) 
 //   e le richieste P2P in ingresso gestite dal PeerServer.

 public class ArchivioLocale {

    // Struttura dati per memorizzare le misurazioni locali (Nome -> Contenuto)
    private final Map<String, String> misurazioni;


    public ArchivioLocale() {
        this.misurazioni = new HashMap<>();
        
        // Pre-allocazione di esempio come consentito dalle specifiche di progetto
        this.misurazioni.put("R0", "Valore=21.5C;Pressione=1012hPa");
        this.misurazioni.put("R1", "CO2=450ppm;Umidita=55%");
    }

    public synchronized void aggiungiMisura(String nome, String contenuto) {
        if (nome != null && !nome.trim().isEmpty() && contenuto != null) {
            this.misurazioni.put(nome.trim(), contenuto);
        }
    }


    public synchronized void aggiungiMisure(String nome, String contenuto) {
        aggiungiMisura(nome, contenuto);
    }


    public synchronized List<String> getLocalData() {
        return new ArrayList<>(this.misurazioni.keySet());
    }

   
    public synchronized String getContent(String nome) {
        if (nome == null) {
            return null;
        }
        return this.misurazioni.get(nome.trim());
    }
}

