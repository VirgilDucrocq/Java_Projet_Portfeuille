import java.io.*;
import java.net.*;
import java.util.*;

public class Serveur{

    private int port;
    private ServerSocket serverSocket;
    private Map<Action, Integer> stockGlobal; // Actions + Quantités
    private List<ThreadClientServeur> clients = new ArrayList<>(); //Clients

    public Serveur(int port, Map<Action, Integer> stockInitial){
        this.port = port;
        this.stockGlobal = new HashMap<>(stockInitial);
    }

    public synchronized List<Action> getActions() {
    // renvoyer une nouvelle liste pour éviter les modifications externes
        return new ArrayList<>(stockGlobal.keySet());
    }

    public void demarrer(){
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Serveur démarré sur le port " + port);

            while (true){
                Socket socketClient = serverSocket.accept();
                ThreadClientServeur tcs = new ThreadClientServeur(socketClient, this);
                clients.add(tcs);
                new Thread(tcs).start();
            }

        } catch (IOException e){
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    
    
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

    public synchronized Map<Action, Integer> getStockGlobal() {
        return this.stockGlobal;
    }

    public int getPort(){
        return this.port;
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        // Création des actions
        Action apple = new Action("Apple", 100);
        Action google = new Action("Google", 200);
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
