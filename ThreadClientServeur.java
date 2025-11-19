import java.io.*;
import java.net.*;
import java.util.*;

public class ThreadClientServeur implements Runnable {

    private Socket socket;
    private Serveur serveur;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ThreadClientServeur(Socket socket, Serveur serveur) throws IOException{
        this.socket = socket;
        this.serveur = serveur;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    public void envoyerMiseAJourActions(List<Action> actions) throws IOException {
    
        this.out.writeObject("UPDATE_ACTIONS"); // Optionnel: envoi d'un marqueur
        this.out.writeObject(actions);
        this.out.flush();
    }

    public void run(){
        try {
            while (true) {
                Object obj = in.readObject();

                if (obj instanceof String cmd && cmd.equals("GET_ACTIONS")) {
                    out.reset();
                    // Envoi de la liste des actions actuelles au client
                    out.writeObject(new HashMap<>(serveur.getStockGlobal())); 
                    out.flush();
                    continue;
                }

                if (obj instanceof Transaction t) {
                    System.out.println("[Serveur] Reçu: " + t);
                    out.reset();
                    serveur.traiterTransaction(t);
                    out.writeObject(t);
                    out.flush();
                    System.out.println("[Serveur] Renvoi: " + t + " " + (t.estAcceptee()? "ACCEPTEE":"REFUSEE"));
                }

                
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client déconnecté : " + e.getMessage());
        }
    }
    
}
