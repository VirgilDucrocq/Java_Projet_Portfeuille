import java.io.*;
import java.net.*;
import java.time.*;
import java.util.Scanner;
import java.util.*;
import javax.swing.SwingUtilities;

//Serializable car les transactions comprennent un client qui va donc etre envoyé par socket
public class Client implements Serializable{

    //Protection contre la corruption à la deserialisation
    private static final long serialVersionUID = 1L;
    private String nom;
    // Transient = ne doit pas être serialisé
    transient private Socket socket;
    private Portefeuille portefeuille;
    //Ici l'écoute et l'envoi sont geres en interne de la classe 
    transient private ObjectOutputStream fluxSortie;
    transient private ObjectInputStream fluxEntree;
    //Va servir a recuperer le stock dispo envoye par le serveur 
    transient private Map<Action, Integer> stockDisponible = Collections.emptyMap();



    public Client(String nom, double soldeInitial){
        this.nom = nom;
        this.portefeuille = new Portefeuille(soldeInitial);
        //On rattache le portefeuille au client en dehors du constructeur pour eviter 
        //les fuites
    }

    //Getters
    public String getNom(){
        return this.nom;
    }

    public Portefeuille getPortefeuille(){
        return this.portefeuille;
    }

    //recuperer le dernier stock dispo
    public Map<Action, Integer> getDernierStockDisponible(){
        return this.stockDisponible;
    }


    //Recherche le nouveau prix d'une action à partir d'une ancienne reference
    // private car utilitaire interne à Client
    private Action getActionMarcheActuel(Action actionReference){
        return this.stockDisponible.keySet().stream()
            .filter(a -> a.getNom().equals(actionReference.getNom()))
            .findFirst()
            .orElse(actionReference); // Retourne la référence si non trouvée
    }

    //Maj des données (stockDisponible) reçu par socket par le serveur
    //Suppression du warning on sait bien quel objet sera envoyé 
    @SuppressWarnings("unchecked")
    public synchronized void synchroniserStockMarche(){
        try{
            fluxSortie.writeObject("GET_ACTIONS");
            fluxSortie.flush();

            Object reponse = fluxEntree.readObject();
        
            // ATTENTION: GÉRER LA RÉCEPTION DE LA MAP !
            if (reponse instanceof Map<?, ?>){
                // Assurez-vous que le casting est sûr
                this.stockDisponible = (Map<Action, Integer>) reponse; 
            } else if (reponse instanceof List<?>){
                // Gérer l'ancien format si nécessaire, sinon erreur
                System.err.println("Réponse inattendue du serveur, attendu Map<Action, Integer>.");
                this.stockDisponible = Collections.emptyMap();
            } else{
                System.err.println("Réponse inattendue du serveur : " + reponse);
                this.stockDisponible = Collections.emptyMap();
            }
        } catch (Exception e){
            System.err.println("Erreur lors de la récupération des actions : " + e.getMessage());
            this.stockDisponible = Collections.emptyMap();
        }
    }

    //fonction principale pour se connecter au serveur 
    public void seConnecter(String hote, int port){
        try{
            socket = new Socket(hote, port);

            fluxSortie = new ObjectOutputStream(socket.getOutputStream());
            fluxSortie.flush();
            fluxEntree = new ObjectInputStream(socket.getInputStream());

            System.out.println(this.getNom() + " connecté à " + hote + ":" + port);
        } catch (IOException e){
            System.err.println("Erreur de connexion: " + e.getMessage());
        }
    }

    //lance le thread de MAJ en interne pour revuperer les actions a jour 
    public void lancerMiseAJourActions(Runnable callback){
        new Thread(() ->{
            System.out.println("Thread de mise à jour des prix démarré.");
            while (true){
                try{
                    Thread.sleep(500); 

                    // Synchronisation pour s'assurer qu'un seul thread accède aux flux
                    synchronized (Client.this){
                        synchroniserStockMarche(); 
                    }
                    
                    if (callback != null){
                        SwingUtilities.invokeLater(callback); 
                    }
                    
                } catch (InterruptedException e){
                    System.out.println("Thread de mise à jour des actions interrompu.");
                    break;
                } catch (Exception e){
                    System.err.println("Erreur dans le thread de mise à jour: " + e.getMessage() + ". Tentative de poursuite.");
                    try{
                        // Pause pour éviter une boucle serrée en cas d'erreur de connexion
                        Thread.sleep(5000); 
                    } catch (InterruptedException ie){
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    //envoie dune demande d'achat au serveur 
    public boolean demanderAchat(Action action, int quantite){
        Action actionActuelle = getActionMarcheActuel(action); 
        double cout = actionActuelle.getPrix() * quantite;
        
        if (portefeuille.getSoldeDispo() < cout){
            System.out.println("Achat refusé côté client : solde insuffisant.");
            return false;
        }
        
        Transaction demande = new Transaction(this.getNom(), actionActuelle, quantite, TypeTransaction.ACHAT, LocalDate.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()){ 
            portefeuille.ajouterAction(actionActuelle, quantite);
            portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() - cout);
            System.out.println("Achat validé : " + quantite + " x " + actionActuelle.getNom() + " achetés au prix de " + String.format("%.2f", actionActuelle.getPrix()) + "€/unité.");
            return true; // Succès: stock marché a diminué
        } else{
            System.out.println("Achat refusé côté serveur : stock insuffisant.");
            return false;
        }
    }


    // envoyer demande de vente au serveur 
    public boolean demanderVente(Action action, int quantite){
        int possede = portefeuille.getPortefeuille().getOrDefault(action, 0);

        // 1. Vérification locale AVANT l'envoi au serveur
        if (possede < quantite){
            System.out.println("Vente refusée côté client : pas assez d'actions.");
            return false; // Échec: transaction n'est PAS envoyée, le stock serveur NE DOIT PAS être affecté
        }

        // Utilisation de la méthode utilitaire pour obtenir le prix marché
        Action actionActuelle = getActionMarcheActuel(action); 
        double prixDeVente = actionActuelle.getPrix();

        Transaction demande = new Transaction(this.getNom(), actionActuelle, quantite, TypeTransaction.VENTE, LocalDate.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()){ 
            // Le serveur valide toujours une vente dans votre logique Serveur.java
            portefeuille.retirerAction(action, quantite);
            portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() + prixDeVente * quantite);
            System.out.println("Vente validée : " + quantite + " x " + actionActuelle.getNom() + " vendus à " + String.format("%.2f", prixDeVente) + "€/unité.");
            return true; // Succès: stock marché a augmenté
        } else{
            System.out.println("Vente refusée côté serveur.");
            return false; 
        }
    }


    //sous programme pour les demandes de vente/achat
    public synchronized Transaction envoyerTransaction(Transaction t){
        try{
            System.out.println("\n[Client] Envoi transaction: " + t);
            fluxSortie.writeObject(t);
            fluxSortie.flush();

            System.out.println("[Client] Attente réponse serveur");
            Transaction result = (Transaction) fluxEntree.readObject();
            System.out.println("[Client] Réponse reçue: " + result + " " + (result.estAcceptee()? "ACCEPTEE":"REFUSEE"));

            return result;

        } catch (Exception e){
            System.err.println("Erreur d’envoi transaction : " + e.getMessage());
            return null;
        }
    }


    //Main : lance linterface graphique

    public static void main(String[] args){


        // Serveur peut être null si offline
        // Serveur serveur = null; // Variable Serveur supprimée car non utilisée ici
        javax.swing.SwingUtilities.invokeLater(() -> new ClientGUI());
    }
}
