import java.io.*;
import java.net.*;
import java.util.*;

public class Serveur{

    //parametres, connexion reseau géré directement dans cette classe
    private int port;
    private ServerSocket serverSocket;
    private Map<Action, Integer> stockGlobal; // Actions + Quantités du marché
    private List<ThreadClientServeur> clients = new ArrayList<>(); //Clients connectés

    //Constructeur port + stock de base
    public Serveur(int port, Map<Action, Integer> stockInitial){
        this.port = port;
        this.stockGlobal = new HashMap<>(stockInitial);
    }

    //getters
    public synchronized Map<Action, Integer> getStockGlobal() {
        return this.stockGlobal;
    }

    public int getPort(){
        return this.port;
    }
    
    public synchronized List<Action> getActions() {
    // renvoyer une nouvelle liste pour éviter les modifications externes
    // C'est juste une mesure de sécurité ici comme on va beaucoup toucher aux actions
        return new ArrayList<>(stockGlobal.keySet());
    }

    //Fonction principale de lancement avec un thread d'écoute à l'affut de la connexion de clients
    public void demarrer(){
        try {
            serverSocket = new ServerSocket(port);
            //Verification du lancement dans le terminal 
            System.out.println("Serveur démarré sur le port " + port);

            while (true){
                //On accepte les clients sans limite de nombre, on les ajoute à la liste et on lance le thread
                //de gestion de la connexion client-serveur pour le nouveau client
                //On pourrait limiter l'afflux de client en fixant le max de client conectés (longueur de la liste)
                //Afin d'éviter les attaques (DoS) mais ici pas besoin vu qu'on reste en local
                Socket socketClient = serverSocket.accept();
                ThreadClientServeur tcs = new ThreadClientServeur(socketClient, this);
                clients.add(tcs);
                new Thread(tcs).start();
            }

        } catch (IOException e){
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    
    //Gestion des transactions coté serveur (on regarde la demande vis-à-vis de notre stock et on valide ou non)
    public synchronized void traiterTransaction(Transaction t){
        Action action = t.getAction();
        int qte = t.getQuantite();

        if (t.getTypeTransaction() == TypeTransaction.ACHAT){
            if (stockGlobal.get(action) >= qte){
                stockGlobal.put(action, stockGlobal.get(action) - qte);
                t.setValide(true);
            } else{
                t.setValide(false);
            }
        } else{ 
            stockGlobal.put(action, stockGlobal.get(action) + qte);
            t.setValide(true);
        }
    }

    
    // Main du Serveur (création à la main en dur des actions disponibles (pourrait être mis dans une procédure
    // avec une interface graphique style panneau de contrôle serveur)
    // Puis creéation d'une instance de serveur qui va appeler sa fonction démarrer et lancer le thread de mise à jour des prix
    public static void main(String[] args) throws IOException, InterruptedException {
        // Création des actions
        Action apple = new Action("Apple", 100, ActionType.VALEUR_TECH_CLASSIQUE);
        Action google = new Action("Google", 200, ActionType.INDICE_STABLE);
        List<Action> actions = Arrays.asList(apple, google);

        // Stock initial côté serveur
        Map<Action, Integer> stockInitial = new HashMap<>();
        stockInitial.put(apple, 20);
        stockInitial.put(google, 20);

        // Création et démarrage du serveur
        Serveur serveur = new Serveur(5001, stockInitial);
        new Thread(() -> {serveur.demarrer();}).start();
        Thread.sleep(500); // laisser le serveur démarrer
 

        // Lancement du thread de mise à jour des prix (toutes les 20 sec)
        MajCoursThread majCours = new MajCoursThread(actions, stockInitial, 2000);
        new Thread(majCours).start();
    }


}
 
