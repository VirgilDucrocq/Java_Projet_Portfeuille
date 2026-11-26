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
    // Constante pour le répertoire de sauvegarde
    private static final String CLIENT_DIR_SAUV = "clients/";
    //Référence au thread de mise à jour des prix (private pour encapsulation) pour la deconnexion
    transient private Thread updateThread;


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
    //Suppression du warning on sait bien quel objet sera envoyé dans la map
    @SuppressWarnings("unchecked")
    public synchronized void synchroniserStockMarche(){
        // Securité, Si le flux de sortie est null, cela signifie 
        // que nous sommes déconnectés donc on arrête ici
        if (fluxSortie == null) {
            this.stockDisponible = Collections.emptyMap(); 
            return; 
        }

        try{
            fluxSortie.writeObject("GET_ACTIONS");
            fluxSortie.flush();

            Object reponse = fluxEntree.readObject();
        
            // gestion reception de la map
            if (reponse instanceof Map<?, ?>){
                // on s'assure que le casting est sûr
                this.stockDisponible = (Map<Action, Integer>) reponse; 
            } else{
                //Balise test
                System.err.println("Réponse inattendue du serveur : " + reponse);
                this.stockDisponible = Collections.emptyMap();
            }

        } catch (Exception e){
            //Balise test
            System.err.println("Erreur lors de la récupération des actions : " + e.getMessage());
            this.stockDisponible = Collections.emptyMap();
        }
    }
    
    public double getValeurTotalePortefeuilleTempsReel() {
        double valeurTotale = 0.0;
        Map<Action, Integer> monPortefeuille = this.portefeuille.getPortefeuille();

        if (monPortefeuille.isEmpty() || this.stockDisponible.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Action, Integer> entree : monPortefeuille.entrySet()) {
            Action actionPossedee = entree.getKey();
            int quantite = entree.getValue();

            // On cherche le prix actuel dans stockDisponible
            double prixActuel = this.stockDisponible.keySet().stream()
                .filter(a -> a.getNom().equals(actionPossedee.getNom()))
                .map(Action::getPrix)
                .findFirst()
                .orElse(0.0); //0 si erreur (cas hors ligne)
            
            valeurTotale += prixActuel * quantite;
        }
        return valeurTotale;
    }

    //fonction principale pour se connecter au serveur 
    public void seConnecter(String hote, int port){
        try{
            socket = new Socket(hote, port);

            fluxSortie = new ObjectOutputStream(socket.getOutputStream());
            fluxSortie.writeObject(this.getNom()); // Envoyer d'abord le nom comme String
            fluxSortie.flush();
            fluxEntree = new ObjectInputStream(socket.getInputStream());
            //Balise test
            System.out.println(this.getNom() + " connecté à " + hote + ":" + port);
            //sauvegarde client
            this.sauvegarderClient();
        } catch (IOException e){
            //Balise test
            System.err.println("Erreur de connexion: " + e.getMessage());
            fluxSortie = null; 
            fluxEntree = null;
            socket = null;
        }
    }

    //lance le thread de MAJ en interne pour recuperer les actions a jour (englobe synchronizerStockMarche
    //Aurait pu être codé dans la même méthode mais nous trouvons plus clair de l'écrire ainsi en décomposant la 
    //fonction de chaque méthode)

    public void lancerMiseAJourActions(Runnable callback){
        Thread majThread = new Thread(() ->{
            //Balise test
            System.out.println("Thread de mise à jour des prix démarré");
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
                    //Balise test
                    System.out.println("Thread de mise à jour des actions interrompu on arrête");
                    //Securité
                    Thread.currentThread().interrupt(); // Réinitialise l'état d'interruption
                    break;
                } catch (Exception e){
                    //Balise test
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
        });
        this.updateThread = majThread; 
        majThread.start();    
    }

    public void deconnecter(){
        //Sauvegarde Client
        this.sauvegarderClient();
        // Arrêt du thread de mise à jour des prix
        if (updateThread != null && updateThread.isAlive()){
            updateThread.interrupt();
            updateThread = null; 
        }

        // Fermeture des flux et de la socket
        // (On ferme bien dans des try+catch séparé pour garantir la tentative de fermeture de toutes les ressources)
        try{
            if (fluxSortie != null) fluxSortie.close();
        } catch (IOException e){
            System.err.println("Erreur lors de la fermeture du flux de sortie : " + e.getMessage());
        }
        try{
            if (fluxEntree != null) fluxEntree.close();
        }catch (IOException e){
            System.err.println("Erreur lors de la fermeture du flux d'entrée : " + e.getMessage());
        }
        try{
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println(this.nom + " déconnecté du marché.");
        }catch (IOException e){
            System.err.println("Erreur lors de la fermeture de la socket : " + e.getMessage());
        }
    
        //Réinitialisation de l'état réseau (pour se reconnecter après)
        socket = null;
        fluxSortie = null;
        fluxEntree = null;
        stockDisponible = Collections.emptyMap(); // Les dernières données du marché ne sont plus valides (d'ici quelques ms)
    }

    //envoie d'une demande d'achat au serveur 
    public boolean demanderAchat(Action action, int quantite){
        Action actionActuelle = getActionMarcheActuel(action); 
        double cout = actionActuelle.getPrix() * quantite;
        
        if (portefeuille.getSoldeDispo() < cout){
            //Balise test
            //System.out.println("Achat refusé côté client : solde insuffisant.");
            return false;
        }
        
        //On crée la transaction et on regarde si elle est validée ou non
        Transaction demande = new Transaction(this.getNom(), actionActuelle, quantite, TypeTransaction.ACHAT, LocalDateTime.now());
        Transaction resultat = envoyerTransaction(demande);

        //si elle est ok
        if (resultat != null && resultat.estAcceptee()){ 
            portefeuille.ajouterAction(actionActuelle, quantite);
            portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() - cout);
            //Sauvegarde du client , au cas ou on crash et qu'il n'y a pas de deconnexion
            //propre. Pour eviter que des actions disparaissent (sauvegarde au fil de l'eau)
            this.sauvegarderClient();
            //Balise test
            //System.out.println("Achat validé : " + quantite + " x " + actionActuelle.getNom() + " achetés au prix de " + String.format("%.2f", actionActuelle.getPrix()) + "€/unité.");
            return true; // Succès: stock marché a diminué
        } else{
            //Balise test
            //System.out.println("Achat refusé côté serveur : stock insuffisant.");
            return false;
        }
    }


    // envoyer demande de vente au serveur 
    public boolean demanderVente(Action action, int quantite){
        int possede = portefeuille.getPortefeuille().getOrDefault(action, 0);

        // Vérification locale de la quantité avant d'envoyer au serveur
        if (possede < quantite){
            //Balise test
            //System.out.println("Vente refusée côté client : pas assez d'actions.");
            return false;
        }

        // Utilisation de la méthode utilitaire pour obtenir le prix marché
        Action actionActuelle = getActionMarcheActuel(action); 
        double prixDeVente = actionActuelle.getPrix();

        Transaction demande = new Transaction(this.getNom(), actionActuelle, quantite, TypeTransaction.VENTE, LocalDateTime.now());
        Transaction resultat = envoyerTransaction(demande);

        if (resultat != null && resultat.estAcceptee()){ 
            // Le serveur valide toujours la vente dans notre logique du moment qu'on a le stock
            portefeuille.retirerAction(action, quantite);
            portefeuille.setSoldeDispo(portefeuille.getSoldeDispo() + prixDeVente * quantite);
            //Sauvegarde du client , au cas ou on crash et qu'il n'y a pas de deconnexion
            //propre. Pour eviter que des actions disparaissent (sauvegarde au fil de l'eau)
            this.sauvegarderClient();
            
            //Balise test
            //System.out.println("Vente validée : " + quantite + " x " + actionActuelle.getNom() + " vendus à " + String.format("%.2f", prixDeVente) + "€/unité.");
            return true; //tout est bon on va augmenter le stock marché 
        } else{
            System.out.println("Vente refusée côté serveur.");
            return false; 
        }
    }


    //sous programme pour la partie commune des demandes de vente/achat 
    public synchronized Transaction envoyerTransaction(Transaction t){
        try{
            //Balise test
            //System.out.println("\n[Client] Envoi transaction: " + t);
            fluxSortie.writeObject(t);
            fluxSortie.flush();
            //Balise test
            //System.out.println("[Client] Attente réponse serveur");
            Transaction result = (Transaction) fluxEntree.readObject();
            //Balise test
            //System.out.println("[Client] Réponse reçue: " + result + " " + (result.estAcceptee()? "ACCEPTEE":"REFUSEE"));
            return result;

        }catch (Exception e){
            //Balise test
            //System.err.println("Erreur d’envoi transaction : " + e.getMessage());
            return null;
        }
    }

    // Méthode de sauvegarde du client (appellée à la déconnexion ou à la fermeture de l'interface)
    public void sauvegarderClient(){
        File dir = new File(CLIENT_DIR_SAUV);
    
        //Vérification et Création du Répertoire
        // Si le répertoire n'existe pas et que la tentative de création échoue, on stop
        if (!dir.exists() && !dir.mkdirs()) { 
            System.err.println("Soucis, impossible de créer le répertoire de sauvegarde: " + CLIENT_DIR_SAUV);
            return; 
        }

        //Sauvegarde de l'Objet (Client)
        String fileName = CLIENT_DIR_SAUV + this.nom + ".ser";

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(this); // Sauvegarde l'objet Client
        } catch (IOException e) {
            System.err.println("Soucis, sauvegarde du client " + this.nom + " : " + e.getMessage());
        }
    }

    // Méthode statique de chargement (liée à la classe et pas l'instance vu que tous les clients la partage )
    public static Client chargerClient(String nomClient) {
        String ficNom = CLIENT_DIR_SAUV + nomClient + ".ser";
        File fic = new File(ficNom);
    
        if (fic.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ficNom))){
                Client clientCharge = (Client) ois.readObject();
            
                // Les champs transient doivent être nuls pour la reconnexion
                clientCharge.socket = null;
                clientCharge.fluxSortie = null;
                clientCharge.fluxEntree = null;
                clientCharge.updateThread = null;
                clientCharge.stockDisponible = Collections.emptyMap();

                System.out.println("Client " + nomClient + " chargé");
                return clientCharge;
            
            }catch (IOException | ClassNotFoundException e){
                System.err.println("Erreur de chargement du client " + nomClient + " Création d'un nouveau client ");
                // Si le chargement échoue, on crée le client
                return null; 
            }
        }
        // Aucun fichier trouvé, on crée un nouveau client
        return null; 
    }

    //Main : lance linterface graphique

    public static void main(String[] args){
        javax.swing.SwingUtilities.invokeLater(() -> new ClientGUI());
    }
}
