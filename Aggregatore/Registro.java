package  Aggregatore;

import java.util.*;

public class Registro {

    // due mappe tenute allineate: una per "chi ha questo dato?,
    // l'altra per "cosa ha questo nodo?" (serve in removeNode)
    private Map<String, Set<String>> nodiCheHanno = new HashMap<>();
    private Map<String, Set<String>> datiDiNodo = new HashMap<>();

    // ip e porta di ogni nodo, per poterlo contattare davvero dopo un TOKEN
    private Map<String, String> ipDiNodo = new HashMap<>();
    private Map<String, String> portaDiNodo = new HashMap<>();

   // registra (o aggiorna) l'elenco delle rilevazioni possedute da un nodo,
    // insieme a dove trovarlo (ip e porta)
    public synchronized void registerData(String nodeId, List<String> dataList, String ip, String porta) {
        // prima togliamo i riferimenti ai dati che il nodo aveva ma non ha più
        Set<String> vecchiDati = datiDiNodo.get(nodeId);
        if (vecchiDati != null) {
            for (String dato : vecchiDati) {
                if (!dataList.contains(dato)) {
                    Set<String> nodi = nodiCheHanno.get(dato);
                    if (nodi != null) {
                        nodi.remove(nodeId);
                        if (nodi.isEmpty()) {
                            nodiCheHanno.remove(dato);
                        }
                    }
                }
            }
        }

        // poi salviamo la nuova lista e aggiorniamo l'indice inverso
        datiDiNodo.put(nodeId, new HashSet<>(dataList));
        for (String dato : dataList) {
            nodiCheHanno.computeIfAbsent(dato, k -> new HashSet<>()).add(nodeId);
        }

        // aggiorniamo anche dove si trova il nodo, nel caso sia cambiato
        ipDiNodo.put(nodeId, ip);
        portaDiNodo.put(nodeId, porta);
    }

    // dato il nome di una rilevazione, dice quali nodi la possiedono

    public synchronized List<String> getNodesForData(String dataName) {
        Set<String> nodi = nodiCheHanno.get(dataName);
        if (nodi == null) {
            // nessuno ce l'ha: meglio una lista vuota che null,
            // così chi chiama non deve controllare ogni volta
            return new ArrayList<>();
        }
        return new ArrayList<>(nodi);
    }

    // toglie completamente un nodo dal registro, insieme a tutte le sue rilevazioni
    // (viene usato sia quando il nodo fa QUIT sia quando la connessione cade di colpo)

    public synchronized void removeNode(String nodeId) {
        Set<String> dati = datiDiNodo.remove(nodeId);
        ipDiNodo.remove(nodeId);
        portaDiNodo.remove(nodeId);
        if (dati == null) {
            return; // il nodo non era nemmeno registrato, non c'è altro da fare
        }
        for (String dato : dati) {
            Set<String> nodi = nodiCheHanno.get(dato);
            if (nodi != null) {
                nodi.remove(nodeId);
                if (nodi.isEmpty()) {
                    nodiCheHanno.remove(dato);
                }
            }
        }
    }

    // toglie solo un singolo dato da un nodo, senza rimuovere il nodo intero:
    // serve per quando un download fallisce (arriva NODE_FAILED da
    // RobustDownloader/GestoreClient) ma il nodo potrebbe avere ancora altro

    public synchronized void removeNodeForData(String nodeId, String dataName) {
        Set<String> nodi = nodiCheHanno.get(dataName);
        if (nodi != null) {
            nodi.remove(nodeId);
        }
        Set<String> dati = datiDiNodo.get(nodeId);
        if (dati != null) {
            dati.remove(dataName);
        }
    }

    // sceglie da quale nodo scaricare una rilevazione (mai da se stessi)
    // e ritorna "id;ip;porta" del nodo scelto, o null se non c'è nessuno

    public synchronized String requestToken(String dataName, String clientNodeId) {
        Set<String> nodi = nodiCheHanno.get(dataName);
        if (nodi == null) {
            return null;
        }
        for (String nodo : nodi) {
            if (!nodo.equals(clientNodeId)) {
                return nodo + ";" + ipDiNodo.get(nodo) + ";" + portaDiNodo.get(nodo);
            }
        }
        return null; // esiste solo il richiedente stesso, o nessuno
    }

    // elenco completo di tutte le rilevazioni sulla rete con i relativi possessori,
    // usato dal comando "listdata" dell'aggregatore
    public synchronized Map<String, List<String>> getAllData() {
        Map<String, List<String>> risultato = new HashMap<>();
        for (Map.Entry<String, Set<String>> voce : nodiCheHanno.entrySet()) {
            risultato.put(voce.getKey(), new ArrayList<>(voce.getValue()));
        }
        return risultato;
    }
}