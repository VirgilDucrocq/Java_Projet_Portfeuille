import java.io.*;
import java.net.*;
import java.util.*;

//gestion de l'écoute (demande d'achat/vente) et de l'envoie des prix, mis à jour par le serveur, avec le client
public class ThreadClientServeur implements Runnable{

    private Socket socket;
    private Serveur serveur;
    private ObjectInputStream fluxEntree;
    private ObjectOutputStream fluxSortie;
    private String clientNom;

    //Constructeur
    public ThreadClientServeur(Socket socket, Serveur serveur) throws IOException{
        this.socket = socket;
        this.serveur = serveur;
        this.fluxSortie = new ObjectOutputStream(socket.getOutputStream());
        this.fluxSortie.flush();
        this.fluxEntree = new ObjectInputStream(socket.getInputStream());
    }


    
    public void run(){
        try {
            boolean nomRecu = false; // Flag pour s'assurer que le nom est bien défini
            while (true){
                Object obj = fluxEntree.readObject();

                if (obj instanceof String cmd){
                
                    if (!nomRecu){
                        // Si c'est le premier String reçu, c'est le nom 
                        this.clientNom = cmd;
                        nomRecu = true;
                        // On ne répond rien, le client est juste identifié
                        continue; 
                    } 
                
                    // Sinon c'est que c'est une commande (on vérifie que c'est bien la seule qu'on ait)
                    if (cmd.equals("GET_ACTIONS")){
                        fluxSortie.reset();
                        fluxSortie.writeObject(new HashMap<>(serveur.getStockGlobal())); 
                        fluxSortie.flush();
                        continue;
                    }
            }

                if (obj instanceof Transaction t){
                    //Balises de contrôle depuis le terminal
                    //System.out.println("[Serveur] Reçu: " + t);
                    fluxSortie.reset();
                    serveur.traiterTransaction(t);
                    fluxSortie.writeObject(t);
                    fluxSortie.flush();
                    //System.out.println("[Serveur] Renvoi: " + t + " " + (t.estAcceptee()? "ACCEPTEE":"REFUSEE"));
                }

                
            }
        }catch (IOException | ClassNotFoundException e){
            // Affichage du nom mémorisé (ou de la valeur par défaut si jamais rien n'a été reçu)
            System.out.println("Client (" + this.clientNom + ") déconnecté");
            try{
                socket.close();
            } catch (IOException closeE){
                System.err.println("Erreur lors de la fermeture de la socket : " + closeE.getMessage());
            }
        }
    }
}
