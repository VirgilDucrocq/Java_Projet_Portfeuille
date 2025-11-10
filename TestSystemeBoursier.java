import java.io.IOException;
import java.util.*;
import java.time.*;

public class TestSystemeBoursier{

    public static void main(String[] args) throws IOException, InterruptedException {

        // Création des actions
        Action apple = new Action("Apple", 100);
        Action google = new Action("Google", 200);
        List<Action> actions = Arrays.asList(apple, google);

        // Stock initial côté serveur
        Map<Action,Integer> stockInit = new HashMap<>();
        stockInit.put(apple, 5);
        stockInit.put(google, 2);

        // Démarrage du serveur
        Serveur serveur = new Serveur(5001, stockInit);
        new Thread(() -> {serveur.demarrer();}).start();
        Thread.sleep(500); // laisser le serveur démarrer
 

        // Lancement du thread de mise à jour des prix (toutes les 2 sec)
        MajCoursThread majCours = new MajCoursThread(actions, stockInit, 2000);
        new Thread(majCours).start();

        // Création des clients
        Client virgil = new Client("Virgil", 1000.0);
        Client thomas = new Client("Thomas", 1000.0);

        // Connexion au serveur
        virgil.seConnecter("localhost", 5001);
        thomas.seConnecter("localhost", 5001);

        Thread.sleep(1000); // Laisser les clients se connecter

        // Transactions tests
        System.out.println("\n--- Transactions ---");
        virgil.demanderAchat(apple, 3);           // devrait passer
        thomas.demanderAchat(google, 6);          // devrait échouer (stock < 6)
        virgil.demanderVente(apple, 1);           // devrait passer

        //  Affichage des portefeuilles
        System.out.println("\n--- Portefeuilles ---");
        System.out.println("Virgil :\n" + virgil.getPortefeuille());
        System.out.println("Thomas :\n" + thomas.getPortefeuille());

        // ;Affichage stock côté serveur
        System.out.println("\n--- Stock serveur ---");
        System.out.println("Apple : " + serveur.getStock(apple));
        System.out.println("Google : " + serveur.getStock(google));

        // Laisser tourner un peu les mises à jour des cours
        Thread.sleep(5000);

        System.exit(0);
    }
}
