package  Aggregatore;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Logger {


    private List<String> voci = new ArrayList<>();
    private static final DateTimeFormatter FORMATO_ORA = DateTimeFormatter.ofPattern("HH:mm");

    // salvo un tentativo di download: chi lo ha chiesto, a chi, cosa e se è andato bene
    // (formato tipo "13:00 R0 da: peer0 a: peer1", come nell'esempio delle specifiche)
    public synchronized void logOperation(String sourceNode, String destNode, String dataName, boolean success) {
        String ora = LocalTime.now().format(FORMATO_ORA);
        String voce = ora + " " + dataName + " da: " + sourceNode + " a: " + destNode;
        if (!success) {
            voce += " [FALLITO]";
        }
        voci.add(voce);
    }
 
    // do una copia della lista, non quella vera, altrimenti da fuori
    // qualcuno potrebbe modificarla e rovinarmi lo stato interno
    public synchronized List<String> getLogs() {
        return new ArrayList<>(voci);
    }

}
