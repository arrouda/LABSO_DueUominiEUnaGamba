package Aggregatore;

import Comunicazione.*;
import java.util.*;

/*
Registro gestisce l'indice centrale sincronizzato delle risorse e dei nodi sensore.
Mantiene la mappatura tra le misurazioni e i nodi che le possiedono, e le informazioni di ciascun nodo come ip e porta.
 
Delego alla classe InfoNodo il compito di salvare e restituire le informazioni dei singoli nodi.
 
Siccome devo essere sicuro che tutte le informazioni sui nodi siano giuste, i metodi di modifica del reistro sono syncronized.
 */

public class Registro {

    private  Map<String, Set<String>> nodiCheHanno = new HashMap<>();
    private  Map<String, Set<String>> datiDiNodo = new HashMap<>();
    private  Map<String, InfoNodo> infoNodi = new HashMap<>();

    
    //Register data inserisce un nuovo nodo e un nuovo elenco di rilevazioni (se presenti)
    public synchronized void registerData(String nodeId, InfoNodo info, List<String> dataList) {

        if (nodeId == null || dataList == null) {
            return;
        }

        if (info != null) {
            infoNodi.put(nodeId, info);
        }

        // Inizializza o recupera l'insieme delle misurazioni per il nodo
        // siccome viene chiamto da register devo gestire sia il caso in cui il nodo viene creato sia il caso in cui aggingo una rilevazione

        Set<String> misurazioniNodo = datiDiNodo.get(nodeId);

            if (misurazioniNodo == null) {
                misurazioniNodo = new HashSet<>();
                datiDiNodo.put(nodeId, misurazioniNodo);
            }
        
        //Il ciclo scorre le rilevazioni, aggiunge quelle completamente nuove e salva per ogni rilevazione i nodi che la possiedono
        for (String misurazione : dataList) {

            if (misurazione != null && !misurazione.trim().isEmpty()) {

                String mClean = misurazione.trim();
                misurazioniNodo.add(mClean);

                // Aggiorna l'indice inverso nodiCheHanno
                Set<String> nodi = nodiCheHanno.get(mClean);

                if (nodi == null) {

                    nodi = new HashSet<>();
                    nodiCheHanno.put(mClean, nodi);

                }
                
                nodi.add(nodeId);
            }
        }
    }


    //SE FUNZIONA SENZA QUESTO CAVARE

    // public synchronized void registerData(String nodeId, List<String> dataList) {
    //     registerData(nodeId, infoNodi.get(nodeId), dataList);
    // }


    /* Siccome nodi che hanno è una lista formata dalle rilevazioni associate a una lista di nodi che la possiedono
    getNodesForData cerca la rilevazione, se c'è restituisce la lista di nodi associata, se no una lista nulla

    Devo fare il metodo sincronizzato perchè le rilevazioni possono essere lette e scritte contemporaneamente da più nodi */

    public synchronized List<String> getNodesForData(String dataName) {

        if (dataName == null || !nodiCheHanno.containsKey(dataName)) {
            return new ArrayList<>();
        }

        return new ArrayList<>(nodiCheHanno.get(dataName));
    }


    /* requestToken è il metodo che viene richiamato concretamente quando GestoreClient riceve il comando REQUEST TOKEN
    Il metodo guarda la lista dei nodi che possiedono una rilevazione, esclude se stesso e restituisce il primo nodo che la possiede */    


    public synchronized String requestToken(String dataName, String clientNodeId) {

        Set<String> nodi = nodiCheHanno.get(dataName);

        if (nodi == null || nodi.isEmpty()) {
            return null;
        }

        for (String nodoId : nodi) {

            // Esclude il nodo richiedente per evitare tentativi di autorichiesta
            if (!nodoId.equals(clientNodeId)) {

                InfoNodo info = infoNodi.get(nodoId);

                //restituisco le informazioni del primo nodo che possiede la rilevazione (escluso se stesso)
                if (info != null) {
                    return nodoId + ";" + info.getIP() + ";" + info.getPort();
                }

            }
        }
        
        // se arrivo alla fine del for-each senza aver trovato un nodo che possiede una rilevazione restituisco un nodo nullo
        return null;
    }


   /*  Quando avviene un node failed richiamo il metodo removeNodeForData che rimuove la rilevazione da quelle del nodo 
    e il nodo dalla lista di nodi che possiedono una specifica rilevazione */

    public synchronized void removeNodeForData(String nodeId, String dataName) {

        if (nodeId == null || dataName == null) {
            return;
        }

        // Rimuove il nodo dall'elenco dei possessori di quella rilevazione
        Set<String> nodi = nodiCheHanno.get(dataName);

        if (nodi != null) {

            nodi.remove(nodeId);

            if (nodi.isEmpty()) {
                nodiCheHanno.remove(dataName);
            }

        }

        // Rimuove la rilevazione dall'elenco delle misurazioni del nodo
        Set<String> dati = datiDiNodo.get(nodeId);

        if (dati != null) {
            dati.remove(dataName);
        }
    }

    
    // remove node rimuove completamente il nodo in caso di quit

    public synchronized void removeNode(String nodeId) {

        if (nodeId == null) {
            return;
        }

        //rimozione del nodo dall'inisieme di nodi
        Set<String> misurazioni = datiDiNodo.remove(nodeId);
        
        // rimozione del nodo da tutte le liste di nodi che possiedono una specifica rilevazione 
        if (misurazioni != null) {

            for (String misurazione : misurazioni) {

                Set<String> nodi = nodiCheHanno.get(misurazione);

                if (nodi != null) {

                    nodi.remove(nodeId);

                    if (nodi.isEmpty()) {
                        nodiCheHanno.remove(misurazione);
                    }

                }
            }
        }

        // Rimuove le informazioni di contatto del nodo
        infoNodi.remove(nodeId);
    }


    // il comando scorre tutte le rilevazioni in nodi che hanno e stampa la lista dei nodi collegati separati da virgola
    @Override
    public synchronized String toString() {

        if (nodiCheHanno.isEmpty()) {
            return "(Nessuna risorsa registrata sulla rete)";
        }

        String risultato = "";

        for (Map.Entry<String, Set<String>> entry : nodiCheHanno.entrySet()) {

            risultato += "- " + entry.getKey() + ": " + String.join(", ", entry.getValue()) + "\n";

        }
        
        return risultato.trim();
    }
}