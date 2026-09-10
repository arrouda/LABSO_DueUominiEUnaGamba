package sensore;

import java.util.*;

//Questa classe:
// Gestisce la memorizzazione locale delle misurazioni (chiave-valore con la Map).
// Utilizza metodi sincronizzati per garantire la mutua esclusione tra thread che  leggono e le misurazioni e thread che ne inseriscono di nuove.
public class ArchivioLocale{
    
    //Questa è la tabella in memoria che conserva le rilevazioni.
    private final Map<String, String> misurazioni;

    public ArchivioLocale(){
        this.misurazioni = new HashMap<>();
    }

    public synchronized void aggiungiMisura(String nome, String contenuto){
        if(nome != null && contenuto != null){
            this.misurazioni.put(nome, contenuto);        
        }
    }

    public synchronized List<String> getLocalData() {
        List<String> listaNomi = new Arraylist<>(this.misurazioni.keySet());
        Collections.sort(listaNomi);
        return listaNomi;
    }

    public synchronized String getContent(String nome){
        return this.misurazioni.get(nome);
    }

}
