NB: AGGIORNARE SEMPRE I COMMENTI, IL TIPO e GLI ARGOMENTI DEI METODI E SCRIVERE ALTRI METODI SE VENGONO AGGIUNTI


SCHEMA CLASSI E METODI:

[aggregatore/]

 Aggregatore.java
	- main
	- startCLI
	- listenForClients

 GestoreClient.java
    - GestoreClient
    - run
    - gestoreRichiesta

 Logger.java
    - logOperation
    - getLogs

 Registro.java
    - registerData
    - getNodesForData
    - removeNode
    - removeNodeForData
    - requestToken

[sensore/]

 ArchivioLocale.java
    - aggiungiMisure
    - getLocalData
    - getContent

 NodoSensore.java
    - main
    - startCLI
    - registrazioneAggregatore
    - disconnettiAggregatore

 PeerRequestHandler.java
    - run

 PeerServer.java
    - run
    - sendFile


[comunicazione/]

 InfoNodo.java
    - getID
    - getIP
    - getPort

 GestoreMessaggi.java
    - serilizedMessage
    - parseMessage

 NetwokClient.java
    - sendRequest

 RobustDownloader.java
    - downloadWhitRetry
    - releaseToken
 

[AGGREGATORE]

LISTA CLASSI/FILE

1) Aggregatore.Java


Aggregatore è la classe principale, il server centrale: crea le risorse condivise (Registro e Logger),
apre il ServerSocket e gestisce sia l'ascolto dei sensori che i comandi da terminale



METODI:

	- public static void main(String[] args)

	legge la porta da riga di comando, crea l'aggregatore con Registro e Logger,
		apre il ServerSocket, avvia il thread di ascolto e poi la CLI
	

	- `private void startCLI()` 
	
	legge i comandi da tastiera (listdata, log, quit) e li esegue


	- `private void listenForClients()
	resta in loop su accept(), per ogni sensore che si connette crea
	un GestoreClient e lo avvia su un thread nuovo
	

2) GestoreClient.Java


GestoreClient gestisce la connessione con un singolo sensore: ogni sensore che si collega
ha la sua istanza su un thread dedicato, che legge i messaggi e li traduce in chiamate al Registro



METODI:


	public GestoreClient(Socket socket, Registro registro, Logger log)` -> costruttore. 

  salva i tre riferimenti nei campi dell'istanza, così sono disponibili agli altri metodi.


	- public void run()

	legge una riga alla volta dal socket, la passa a gestoreRichiesta
	e rimanda la risposta; se la connessione cade toglie il nodo dal registro



	private String gestoreRichiesta(String richiesta)
	capisce il comando (REGISTER, NODES_FOR, TOKEN, QUIT) e chiama il metodo
	giusto sul Registro, poi restituisce la risposta da mandare al sensore



3) Logger.Java


Logger tiene la cronologia delle richieste di download tra sensori,
usata dal comando "log" dell'aggregatore


METODI:

	- public synchronized void logOperation(String sourceNode, String destNode, String dataName, boolean success) {

    		aggiunge una voce al log registrando timestamp, nodi coinvolti,
    		rilevazione scaricata ed esito del download

	  }

	

	- public synchronized List<String> getLogs(){

        	ritorna una copia della lista delle voci registrate
	  }

    	  



4) Registro.Java


Registro tiene traccia di quali nodi possiedono quali rilevazioni e dove si trovano
(ip e porta), così l'aggregatore sa chi proporre a chi


METODI:


	- public synchronized void registerData(String nodeId, List<String> dataList){

		regsitra o aggiorna l'elenco dei dati/ rilevazioni posseduti da uno 
		specifico sensore

	  }


 	- public synchronized List<String> getNodesForData(String dataName){

		ritorna l'elenco (o il primo nodo disponibile) dei sensori che possiedono
        	una determinata rilevazione
        
	  }


	- public synchronized void removeNode(String nodeId){

        	rimuove un nodo e tutte le sue rilevazioni dal registro
        	(disconnessione regolare quit o anomala)

	}



	- public synchronized void removeNodeForData(String nodeId, String dataName){

        	rimuove il riferimento a un singolo tentativo di download fallito
        	quando un nodo risulta irraggiungibile

	  }


 
	- public synchronized String requestToken(String dataName, String clientNodeId){

        	rilascia l'autorizzazione/token per iniziare il download di una specifica rilevazione
	  }








[SENSORE]

LISTA CLASSI/FILE


1) ArchivioLocale.java


[Aggiungere commento ruolo della classe]


METODI:


	- public static void aggiungiMisure(String nome, String contenuto){
		
		SALVA UNA NUOVA RILEVAZIONE ILN LOCALE E NOTIFICA L'AGGREGATORE
    	  }



	- public static void getLocalData(){
        	RITORNA TUTTE LE RILEVAZIONI LOCALI
    	  }



    	- public static void getContent(String nome){
        	RITORNA IL CONTENUTO DEL FILE CON NOME DATO
    	  }


2) NodoSensore.java


[Aggiungere commento ruolo della classe]


METODI:


	- public static void main(String[] args){

        	avvio il sensore, inizializzo l'archivio locale e avvio il peer
		server in background per erogare dati e si registra all'aggregatore

	  }


	- public static void startCLI(){

		interfaccia da terminale per leggere comandi utente

	  }


	- public static void registrazioneAggregatore(){

		registrare il sensore all'aggregatore, avvisa l'aggregatore e
		gli trasmette la lista dei dati locali

	  }


	- public static void disconnettiAggregatore() {

		notifica l'aggregatore della disconnesione prima di chiudere il programma

	  }



3) PeerRequestHandler.java


[Aggiungere commento ruolo della classe]


METODI:

	- public void run() {

		gestisce l'interazioen diretta di invio del
		file sulla singola socket verso il sensore che ha richiesto il download

	  }



4) PeerServer.java


[Aggiungere commento ruolo della classe]


METODI:


	- public void run() {

		mantiene attivo un server socket per il trasferimento peer to peer


	  }


	- synchronized void sendFile(Socket socket, String Nome) {

		invia rilevazione richiesta via socket garantendo di servire una sola
		richiesta (accodo le altre attraverso la sincronizzazione)

	  }








[COMUNICAZIONE]

LISTA CLASSI/FILE


1) InfoNodo.java


InfoNodo è una classe che mi permette di creare oggetti con le informazioni relative ai nodi sensore.
Mi serve per ottenere le iformazioni ai nodi sensore in altre classi come downloader o aggregatore senza dover accedere all'intera classe del nodo sensore.

METODI:


	- public InfoNodo(String id, String ip, int porta){

		costruttore

	  }

	
	- public String getIP(){

		ritorna IP

	  }


	- public String getID(){

		ritorna ID

	  }

	- public int getPort(){

		ritorna porta
		
	  }


2) GestoreMessaggi.java


/* Gestore è una classe che mi permette di trasformare i comandi in stringhe per TCP e le stringhe ricevute TCP in comandi con i rispettivi parametri
Per farlo uso una mappa che contiene coppie di stringhe parametro-valore;
*/


METODI:

	- public static String serilizedMessage(String command, Map<String, String> arguments){

		trasfora il messaggio nel formato giusto per poter viaggiare su TCP */

	  }

	- public static Map<String, String> parseMessage(String message){
		elabora il messaggio ricevuto
		e lo scompone nel comando originale e i relativi parametri
	  }


3) NetwokClient.java


NetworkClient è una classe che permette ai client di creare dei socket con il server, di inviare richieste e ricevere le risposte
Alla fine del metodo sendRequest (il metodo per inviare richieste tramite socket al server), restituisco al chiamate la risposta del server.


METODI:

	- public static String sendRequest(String ip, int porta, String message){

		apro un socket con il destinatario attraverso la porta e l'ip; leggo il messaggio, lo invio e leggo la risposta

		Uso i metodi BufferReader e PrintWriter per facilita ed efficientare la lettura e la scrittura tramite socket.
		BufferReader e PrintWriter Avvolgono infatti degli oggetti di tipo InputStream e OutputStream e mi permettono di leggere e scrivere per righe invece che per singoli caratteri.

		(vedi try-catch)

	  }


4) RobustDownloader.java


[Aggiungere commento ruolo della classe]


METODI:

	- public static void downloadWhitRetry(String fileName) {

		richiede il token e l'indirizzo del peer all'aggregatore; 
		tenta la connessione verso il sensore sorgente; 
		se fallisce notifica l'aggregatore per rimuovere l'entry e riprova con un altro nodo

	  }


 	- public static void releaseToken(String dataName, String sourceNode, boolean success) {

 		libera il token comunicando all'aggregatore l'esito del download

 	}

