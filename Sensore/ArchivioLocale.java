package Sensore;

import java.util.*;

//Questa classe:
// Gestisce la memorizzazione locale delle misurazioni (chiave-valore con la Map).
// Utilizza metodi sincronizzati per garantire la mutua esclusione tra thread che  leggono e le misurazioni e thread che ne inseriscono di nuove.
public class ArchivioLocale{
    
    //Questa e' la tabella in memoria che conserva le rilevazioni.
    private final Map<String, String> misurazioni;

    public ArchivioLocale(){
        this.misurazioni = new HashMap<>();
    }

    //Salvo o aggiorno una misurazione nell'archivio locale.
    public synchronized void aggiungiMisura(String nome, String contenuto){
        if(nome != null && contenuto != null){
            this.misurazioni.put(nome, contenuto);        
        }
    }

    //Ritorno l'elenco di tutte le rilevazioni possedute localmente e restituisco una copia della lista per evitare concorrenza durante l'iterazione.
    public syncronized List<String> getLocalData() {
        List<String> listaNomi = new Arraylist<>(this.misurazioni.keySet());
        Collections.sort(listaNomi);
        return listaNomi;
    }

    //Recupero il contenuto di una specifica misurazione dato il suo nome e ritorno null se la misurazione non e' presente.
    public synchronized String getContent(String nome){
        return this.misurazioni.get(nome);
    }

}
