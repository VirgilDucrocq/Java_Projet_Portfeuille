import java.io.*;
import java.net.*;
import java.time.*;
import java.util.Scanner;
import java.util.*;
import javax.swing.SwingUtilities;

//Serializable car les transactions comprennent un client qui va donc etre envoyé par socket
public class Client implements Serializable {

    
    private String name;
    private Socket socket;
    private Portefeuille portefeuille;
    //Ici l'écoute et l'envoi sont geres en interne de la classe 
    private ObjectOutputStream out;
    private ObjectInputStream in;
    //Va servir a recuperer le stock dispo envoye par le serveur 
    private Map<Action, Integer> stockDisponible = Collections.emptyMap();



    public Client(String name, double soldeInitial){
        this.name = name;
        this.portefeuille = new Portefeuille(this, soldeInitial);
    }

    //Getters
    public String getName(){
        return this.name;
    }

    public Portefeuille getPortefeuille(){
        return this.portefeuille;
    }

    //recuperer le dernier stock dispo
    public Map<Action, Integer> getLastStockDisponible() {
        return this.stockDisponible;
    }

    //recuperer les actions dans la variable apres l'avoir remplie 
    public List<Action> getLastActionsDisponibles() {
        // FIX: Retourne la List<Action> à partir des clés de la Map stockDisponible
        return new ArrayList<>(this.stockDisponible.keySet()); 
    }


    //On ne ToString pas le portefeuille car il ne connait pas les prix du marché
    //Méthode pour pouvoir obtenir les valeurs du marché (depuis client) et les repertorier
    //Selon les actions du portefeuille (du client aussi donc) 
    public String getPortefeuilleString() {
        // Récupère la liste des actions disponibles (prix du marché)
        List<Action> actionsMarche = this.getLastActionsDisponibles(); 
    
        // Vérifie si le portefeuille du client est vide
        if (this.portefeuille.getPortefeuille().isEmpty()) {
            return "Solde disponible: " + String.format("%.2f €", this.portefeuille.getSoldeDispo());
        }// (Si oui affichage du solde seulement du coup)

        // Vérifie si les prix du marché sont disponibles
        if (actionsMarche.isEmpty()) {
            // Non connecté ou pas d'actions sur le marché (normalement jamais sauf cas limite)
            return "Pas d'actions disponibles, Connectez vous !";

        } else {
            //Cas où tout est good
            return this.portefeuille.toMarketValueString(actionsMarche); 
        }
    }


    //recuperer le stock du serveur
    public synchronized void getActionsDisponibles() { // Modifier le nom en getStockDisponible() serait plus clair
        try {
            out.writeObject("GET_ACTIONS");
            out.flush();

            Object reponse = in.readObject();
        
            // ATTENTION: GÉRER LA RÉCEPTION DE LA MAP !
            if (reponse instanceof Map<?, ?>) {
                // Assurez-vous que le casting est sûr
                this.stockDisponible = (Map<Action, Integer>) reponse; 
            } else if (reponse instanceof List<?>){
                // Gérer l'ancien format si nécessaire, sinon erreur
                System.err.println("Réponse inattendue du serveur, attendu Map<Action, Integer>.");
                this.stockDisponible = Collections.emptyMap();
            } else {
                System.err.println("Réponse inattendue du serveur : " + reponse);
                this.stockDisponible = Collections.emptyMap();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des actions : " + e.getMessage());
            this.stockDisponible = Collections.emptyMap();
        }
    }

    //fonction principale pour se connecter au serveur 
    public void seConnecter(String host, int port){
        try {
            socket = new Socket(host, port);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println(this.getName() + " connecté à " + host + ":" + port);
        } catch (IOException e) {
            System.err.println("Erreur de connexion: " + e.getMessage());
        }
    }

    //lance le thread de MAJ en interne pour revuperer les actions a jour 
    public void lancerMiseAJourActions(Runnable callback) {
        new Thread(() -> {
            System.out.println("Thread de mise à jour des prix démarré.");
            while (true) {
                try {
                    Thread.sleep(500); 

                    // Synchronisation pour s'assurer qu'un seul thread accède aux flux
                    synchronized (Client.this) {
                        getActionsDisponibles(); 
                    }
                    
                    if (callback != null) {
                        SwingUtilities.invokeLater(callback); 
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println("Thread de mise à jour des actions interrompu.");
                    break;
                } catch (Exception e) {
                    System.err.println("Erreur dans le thread de mise à jour: " + e.getMessage() + ". Tentative de poursuite.");
                    try {
                        // Pause pour éviter une boucle serrée en cas d'erreur de connexion
                        Thread.sleep(5000); 
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    //envoie dune demande d'achat au serveur 
    public boolean demanderAchat(Action action, int quantite){
        double cout = action.getPrix() * quantite;
        
        if (portefeuille.getSoldeDispo() < cout) {
            System.out.println("Achat refusé côté client : solde insuffisant.");
            return false;
        }
        
        Transaction demande = new Transaction(this.getName(), action, quantite, TypeTransaction.ACHAT, LocalDate.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()) { 
            portefeuille.ajouterAction(action, quantite);
            portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() - cout);
            System.out.println("Achat validé : " + quantite + " x " + action.getName() + " achetés.");
            return true; // Succès: stock marché a diminué
        } else {
            System.out.println("Achat refusé côté serveur : stock insuffisant.");
            return false;
        }
    }


    // envoyer demande de vente au serveur 
    public boolean demanderVente(Action action, int quantite){
        int possede = portefeuille.getPortefeuille().getOrDefault(action, 0);

        // 1. Vérification locale AVANT l'envoi au serveur
        if (possede < quantite) {
            System.out.println("Vente refusée côté client : pas assez d'actions.");
            return false; // Échec: transaction n'est PAS envoyée, le stock serveur NE DOIT PAS être affecté
        }

        Transaction demande = new Transaction(this.getName(), action, quantite, TypeTransaction.VENTE, LocalDate.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()) { 
            // Le serveur valide toujours une vente dans votre logique Serveur.java
            portefeuille.retirerAction(action, quantite);
            portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() + action.getPrix() * quantite);
            System.out.println("Vente validée : " + quantite + " x " + action.getName() + " vendus.");
            return true; // Succès: stock marché a augmenté
        } else {
            System.out.println("Vente refusée côté serveur.");
            return false; 
        }
    }


    //sous programme pour les demandes de vente/achat
    public synchronized Transaction envoyerTransaction(Transaction t) {
        try {
            System.out.println("\n[Client] Envoi transaction: " + t);
            out.writeObject(t);
            out.flush();

            System.out.println("[Client] Attente réponse serveur");
            Transaction result = (Transaction) in.readObject();
            System.out.println("[Client] Réponse reçue: " + result + " " + (result.estAcceptee()? "ACCEPTEE":"REFUSEE"));

            return result;

        } catch (Exception e) {
            System.err.println("Erreur d’envoi transaction : " + e.getMessage());
            return null;
        }
    }


    //Main : lance linterface graphique

    public static void main(String[] args) {


        // Serveur peut être null si offline
        Serveur serveur = null; 
        javax.swing.SwingUtilities.invokeLater(() -> new ClientGUI());
    }
}



