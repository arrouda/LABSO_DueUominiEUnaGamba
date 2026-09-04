import java.util.List;

public class Registro {

    public synchronized void registerData(String nodeId, List<String> dataList){

    //regsitra o aggiorna l'elenco dei dati/ rilevazioni posseduti da uno 
    // specifico sensore
    }

    public synchronized List<String> getNodesForData(String dataName) {
        // ritorna l'elenco (o il primo nodo disponibile) dei sensori che possiedono
        // una determinata rilevazione
        return null;
    }

    public synchronized void removeNode(String nodeId) {
        // rimuove un nodo e tutte le sue rilevazioni dal registro
        // (disconnessione regolare quit o anomala)
    }

    public synchronized void removeNodeForData(String nodeId, String dataName) {
        // rimuove il riferimento a un singolo tentativo di download fallito
        // quando un nodo risulta irraggiungibile
    }
 
    public synchronized String requestToken(String dataName, String clientNodeId) {
        // rilascia l'autorizzazione/token per iniziare il download di una specifica rilevazione
        return null;
    }
}












}
