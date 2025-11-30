import java.io.*;
import java.net.*;
import java.util.*;

public class Serveur{

    //parametres, connexion reseau géré directement dans cette classe
    private int port;
    private ServerSocket socketServeur;
    private Map<Action, Integer> stockGlobal; // Actions + Quantités du marché
    private List<ThreadClientServeur> clients = new ArrayList<>(); //Clients connectés
    private final List<Transaction> historiqueTransactions = Collections.synchronizedList(new ArrayList<>());
    //Fichier binaire pour charger les données
    private static final String FICHIER_BINAIRE = "historique_binaire.ser"; 
    // Fichier Texte (Lisible) pour le log de transactions
    private static final String FICHIER_LOG_LISIBLE = "transactions_lisibles.txt";
    private static final String FICHIER_STOCK = "stock_binaire.ser";

    //Constructeur port + stock de base
    public Serveur(int port, Map<Action, Integer> stockInitial){
        this.port = port;
        this.stockGlobal = new HashMap<>(stockInitial);
    }

    //getters
    public synchronized Map<Action, Integer> getStockGlobal(){
        return this.stockGlobal;
    }

    public int getPort(){
        return this.port;
    }

    public List<Transaction> getHistoriqueTransactions(){
        // Retourne une vue non modifiable pour éviter les effets externes
        return Collections.unmodifiableList(historiqueTransactions);
    }
    
    public synchronized List<Action> getActions(){
    // renvoyer une nouvelle liste pour éviter les modifications externes ici aussi
    // (juste une mesure de sécurité ici comme on va beaucoup toucher aux actions dans la maj des prix)
        return new ArrayList<>(stockGlobal.keySet());
    }

    //Fonction principale de lancement avec un thread d'écoute à l'affut de la connexion de clients
    public void demarrer(){
        try{
            socketServeur = new ServerSocket(port);
            //Verification du lancement dans le terminal 
            System.out.println("Serveur démarré sur le port " + port);

            while (true){
                //On accepte les clients sans limite de nombre, on les ajoute à la liste et on lance le thread
                //de gestion de la connexion client-serveur pour le nouveau client
                //On pourrait limiter l'afflux de client en fixant le max de client conectés (longueur de la liste puis logique FIFO ou autre)
                //Afin d'éviter les attaques (DoS) mais ici pas besoin vu qu'on reste en local 
                Socket socketClient = socketServeur.accept();
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
            Integer stockActuel = stockGlobal.getOrDefault(action, 0); 
            
            if (stockActuel >= qte){
                stockGlobal.put(action, stockActuel - qte);
                t.setValide(true);
            } else{
                t.setValide(false);
            }
        } else{ 
            // si vente, on ajoute au stock, toujours valide
            Integer stockActuel = stockGlobal.getOrDefault(action, 0); 
            stockGlobal.put(action, stockActuel + qte);
            t.setValide(true);
        }
        historiqueTransactions.add(t);
        logTransactionLisible(t);
    }


    // Méthode de sauvegarde (appelée par le Shutdown Hook)
    public void sauvegarderHistorique(){
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FICHIER_BINAIRE))) {
            // Sauvegarde la liste entière en une seule fois
            oos.writeObject(new ArrayList<>(historiqueTransactions)); 
            System.out.println("Historique des transactions sauvegardé sur le fichier binaire");
        }catch (IOException e){
            System.err.println("Erreur lors de la sauvegarde binaire de l'historique : " + e.getMessage());
        }
    }

    // Méthode de chargement (appelée au démarrage)
    // On sait que le warning est inoffensif ici
    @SuppressWarnings("unchecked")
    public void chargerHistorique(){
        File file = new File(FICHIER_BINAIRE);
        if (file.exists()){
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FICHIER_BINAIRE))){
                List<Transaction> loadedList = (List<Transaction>) ois.readObject();
                historiqueTransactions.addAll(loadedList);
                System.out.println("Historique des transactions chargé (" + loadedList.size() + " entrées) depuis le binaire");
            }catch (IOException | ClassNotFoundException e){
                System.err.println("Erreur lors du chargement de l'historique binaire : " + e.getMessage());
            }
        }
    }

    //Pas dans le diagramme des classes car cette methode revient à sauvegarderhistorique
    //Mais en version lisible par l'humain
    public void logTransactionLisible(Transaction t){
        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHIER_LOG_LISIBLE, true))){
            // Utilisation de ChronoUnit pour tronquer l'heure aux secondes
            String dateHeureTronquee = t.getDateHeure().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
            pw.println(t.toString());
        }catch (IOException e){
            System.err.println("Erreur lors de l'écriture du log lisible : " + e.getMessage());
        }
    }

    public void sauvegarderStock() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FICHIER_STOCK))) {
            oos.writeObject(stockGlobal); // On écrit la Map entière directement
            System.out.println("État du stock sauvegardé dans " + FICHIER_STOCK);
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde stock : " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Action, Integer> chargerStock() {
        File file = new File(FICHIER_STOCK);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FICHIER_STOCK))) {
                return (Map<Action, Integer>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur chargement stock : " + e.getMessage());
            }
        }
        return null; // Retourne null si pas de fichier ou erreur
    }
    
    // Main du Serveur (création à la main en dur des actions disponibles (pourrait être mis dans une procédure
    // avec une interface graphique style panneau de contrôle serveur ou même une base de données style de ce qu'on a fait avec Clients)
    // Puis creéation d'une instance de serveur qui va appeler sa fonction démarrer et lancer le thread de mise à jour des prix
    public static void main(String[] args) throws IOException, InterruptedException{
        // Création des actions
        Action apple = new Action("Apple", 100, ActionType.VALEUR_TECH_CLASSIQUE);
        Action google = new Action("Google", 200, ActionType.INDICE_STABLE);
        Action tesla = new Action("Tesla", 450, ActionType.VALEUR_TECH_CLASSIQUE); 
        Action bitcoin = new Action("Bitcoin", 30000, ActionType.ACTION_VOLATILE_CRYPTO); 
        Action meta = new Action("Meta", 150, ActionType.INDICE_STABLE);
        Action obligationFR = new Action("Obligation FR", 50, ActionType.OBLIGATION_ETAT);
        Action pfizer = new Action("Pfizer", 80, ActionType.INDICE_STABLE); // Stabilité
        Action cac40 = new Action("CAC 40", 500, ActionType.INDICE_STABLE); // Indice européen
        Action startupAI = new Action("Startup AI", 25, ActionType.ACTION_VOLATILE_CRYPTO); // Très forte volatilité et faible prix
        Action or = new Action("Or Bullion", 1800, ActionType.INDICE_STABLE); // Matière première, stable/refuge
        Action toyota = new Action("Toyota", 120, ActionType.VALEUR_TECH_CLASSIQUE); // Classique Automobile
        Action obligationCorp = new Action("Obligation Corp", 65, ActionType.OBLIGATION_HAUT_RENDEMENT); 
        Action cuivre = new Action("Cuivre", 90, ActionType.MATIERE_PREMIERE_INDUSTRIELLE);

        Map<Action, Integer> stockInitial = Serveur.chargerStock();
        
        if (stockInitial == null) {
            System.out.println("Aucune sauvegarde de stock trouvée, initialisation par défaut");
            stockInitial = new HashMap<>();
            stockInitial.put(apple, 20);
            stockInitial.put(google, 20);
            stockInitial.put(tesla, 15);      
            stockInitial.put(bitcoin, 5);      
            stockInitial.put(meta, 30);
            stockInitial.put(obligationFR, 50);
            stockInitial.put(pfizer, 25);
            stockInitial.put(cac40, 10);      
            stockInitial.put(startupAI, 100);   
            stockInitial.put(or, 5);          
            stockInitial.put(toyota, 20);
            stockInitial.put(obligationCorp, 40); 
            stockInitial.put(cuivre, 35);
        }else {
            System.out.println("Stock restauré depuis la dernière sauvegarde !");
        }

        // Création et démarrage du serveur
        Serveur serveur = new Serveur(5001, stockInitial);
        serveur.chargerHistorique();
        new Thread(() ->{serveur.demarrer();}).start();
        Thread.sleep(500);

        // Lancement du thread de mise à jour des prix (toutes les 2 sec)
        MajCoursThread majCours = new MajCoursThread(serveur, 2000); 
        new Thread(majCours).start();
        //Va recuperer juste avant la fermeture (Shutdown hook)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        serveur.sauvegarderHistorique();
        serveur.sauvegarderStock();
        }));
    }
}
