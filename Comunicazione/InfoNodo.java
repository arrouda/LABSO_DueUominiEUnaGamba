package Comunicazione;

public class InfoNodo {

    /*
     * InfoNodo è una classe che serve per creare oggetti
     * che contengano le informazioni dei nodi sensore del sistema
     * in modo che le altre classi come il downloader e l'Aggregatore
     * possano richiamarle quando servono, senza dover accedere a tutta la classe
     * del NodoSensore
     */

    private String id;
    private String ip;
    private int porta;

    public InfoNodo(String id, String ip, int porta) {
        this.id = id;
        this.ip = ip;
        this.porta = porta;
    }

    public String getID() {
        return id;
    }

    public String getIP() {
        return ip;
    }

    public int getPort() {
        return porta;
    }

}
