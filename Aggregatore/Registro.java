package Aggregatore;

import Comunicazione.*;
import java.util.*;

/**
 * Registro gestisce l'indice centrale sincronizzato delle risorse e dei nodi sensore[cite: 1, 3].
 * Mantiene la mappatura tra le misurazioni e i nodi che le possiedono, e le informazioni
 * di contatto (IP, porta P2P) di ciascun nodo sensore[cite: 1].
 */
public class Registro {

    // Map: nomeMisurazione -> Set di ID dei nodi che la possiedono[cite: 1]
    private final Map<String, Set<String>> nodiCheHanno = new HashMap<>();

    // Map: ID nodo -> Set delle misurazioni possedute da quel nodo[cite: 1]
    private final Map<String, Set<String>> datiDiNodo = new HashMap<>();

    // Map: ID nodo -> InfoNodo (contiene ID, IP e Porta P2P)[cite: 1]
    private final Map<String, InfoNodo> infoNodi = new HashMap<>();

    
    public synchronized void registerData(String nodeId, InfoNodo info, List<String> dataList) {
        if (nodeId == null || dataList == null) {
            return;
        }

        if (info != null) {
            infoNodi.put(nodeId, info);
        }

        // Inizializza o recupera l'insieme delle misurazioni per il nodo[cite: 1]
        Set<String> misurazioniNodo = datiDiNodo.computeIfAbsent(nodeId, k -> new HashSet<>());

        for (String misurazione : dataList) {
            if (misurazione != null && !misurazione.trim().isEmpty()) {
                String mClean = misurazione.trim();
                misurazioniNodo.add(mClean);

                // Aggiorna l'indice inverso nodiCheHanno[cite: 1]
                nodiCheHanno.computeIfAbsent(mClean, k -> new HashSet<>()).add(nodeId);
            }
        }
    }

    public synchronized void registerData(String nodeId, List<String> dataList) {
        registerData(nodeId, infoNodi.get(nodeId), dataList);
    }

    public synchronized List<String> getNodesForData(String dataName) {
        if (dataName == null || !nodiCheHanno.containsKey(dataName)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(nodiCheHanno.get(dataName));
    }

    public synchronized String requestToken(String dataName, String clientNodeId) {
        Set<String> nodi = nodiCheHanno.get(dataName);
        if (nodi == null || nodi.isEmpty()) {
            return null;
        }

        for (String nodoId : nodi) {
            // Esclude il nodo richiedente per evitare tentativi di autorichiesta[cite: 1]
            if (!nodoId.equals(clientNodeId)) {
                InfoNodo info = infoNodi.get(nodoId);
                if (info != null) {
                    return nodoId + ";" + info.getIP() + ";" + info.getPort();
                }
            }
        }
        return null;
    }

    public synchronized void removeNodeForData(String nodeId, String dataName) {
        if (nodeId == null || dataName == null) {
            return;
        }

        // Rimuove il nodo dall'elenco dei possessori di quella rilevazione[cite: 1]
        Set<String> nodi = nodiCheHanno.get(dataName);
        if (nodi != null) {
            nodi.remove(nodeId);
            if (nodi.isEmpty()) {
                nodiCheHanno.remove(dataName);
            }
        }

        // Rimuove la rilevazione dall'elenco delle misurazioni del nodo[cite: 1]
        Set<String> dati = datiDiNodo.get(nodeId);
        if (dati != null) {
            dati.remove(dataName);
        }
    }

    
    public synchronized void removeNode(String nodeId) {
        if (nodeId == null) {
            return;
        }

        // Rimuove il nodo da tutte le rilevazioni in nodiCheHanno[cite: 1]
        Set<String> misurazioni = datiDiNodo.remove(nodeId);
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

        // Rimuove le informazioni di contatto del nodo[cite: 1]
        infoNodi.remove(nodeId);
    }

    @Override
    public synchronized String toString() {
        if (nodiCheHanno.isEmpty()) {
            return "(Nessuna risorsa registrata sulla rete)";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : nodiCheHanno.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ")
              .append(String.join(", ", entry.getValue()))
              .append("\n");
        }
        return sb.toString().trim();
    }
}