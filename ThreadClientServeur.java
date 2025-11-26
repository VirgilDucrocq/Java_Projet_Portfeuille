import java.io.*;
import java.net.*;
import java.util.*;

//gestion de l'écoute (demande d'achat/vente) et de l'envoie des prix, mis à jour par le serveur, avec le client
public class ThreadClientServeur implements Runnable {

    private Socket socket;
    private Serveur serveur;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    //Constructeur
    public ThreadClientServeur(Socket socket, Serveur serveur) throws IOException{
        this.socket = socket;
        this.serveur = serveur;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
    }


    
    public void run(){
        try {
            while (true) {
                Object obj = in.readObject();

                //Si on nous demande les actions, on envoie le stock du serveur (nouvelle instance pour être sûr de ne rien modifier, sécurité)
                if (obj instanceof String cmd && cmd.equals("GET_ACTIONS")) {
                    out.reset();
                    out.writeObject(new HashMap<>(serveur.getStockGlobal())); 
                    out.flush();
                    continue;
                }

                if (obj instanceof Transaction t) {
                    //Balises de contrôle depuis le terminal
                    //System.out.println("[Serveur] Reçu: " + t);
                    out.reset();
                    serveur.traiterTransaction(t);
                    out.writeObject(t);
                    out.flush();
                    //System.out.println("[Serveur] Renvoi: " + t + " " + (t.estAcceptee()? "ACCEPTEE":"REFUSEE"));
                }

                
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client déconnecté : " + e.getMessage());
        }
    }
    
}
