import java.util.Random;

//Classe qui va gérer le thread de mise à jour des cours -> doit obligatoirement implémenter runnable donc
public class MajCoursThread implements Runnable{

    private Serveur serveur; // <--- Remplacement par l'objet Serveur
    private int intervalleMillis;              // temps entre maj en ms
    private Random generateurAleatoire;         // Besoin d'aléatoire

    //Constructeur mis à jour
    public MajCoursThread(Serveur serveur, int intervalleMillis){ // <-- Le constructeur prend Serveur
        this.serveur = serveur;
        this.intervalleMillis = intervalleMillis;
        this.generateurAleatoire = new Random();
    }

    //Calcul du prix à l'aide du modèle (reste inchangé)
    public double calculerNouveauPrix(double prixActuel, double mu, double sigma, double dtAnnee){
        double dt = dtAnnee;

        double termeStochastique = sigma * Math.sqrt(dt) * generateurAleatoire.nextGaussian();
        double termeDerive = (mu - (sigma * sigma) / 2.0) * dt;

        double nouveauPrix = prixActuel * Math.exp(termeDerive + termeStochastique);

        return nouveauPrix;
    }

    //run pour le thread (logique mise à jour)
    public void run(){
        final double SECONDES_PAR_AN = 365.25;
        final double dtAnnee = (intervalleMillis / 1000.0) / SECONDES_PAR_AN;

        while (true){
            try{
                Thread.sleep(intervalleMillis);

                // Utilisation de la synchronisation sur l'objet Serveur 
                // ou sur la ressource partagée gérée par le serveur (stockGlobal)
                synchronized (serveur.getStockGlobal()){ // <-- Accès synchronisé à la ressource du Serveur
                    // On récupère la liste d'actions via le getter du Serveur
                    for (Action action : serveur.getActions()){ // <-- Utilisation de serveur.getActions()
                        double mu = action.getMu();
                        double sigma = action.getSigma();

                        double nouveauPrix = Math.floor(100*calculerNouveauPrix(action.getPrix(), mu, sigma, dtAnnee)) / 100.0;

                        // La modification des prix se fait directement sur l'objet Action
                        // qui est une clé de la Map stockGlobal du Serveur.
                        action.setPrix(nouveauPrix);
                    }
                }

            } catch (InterruptedException e){
                System.out.println("Thread de mise à jour des prix interrompu.");
                break;
            }
        }
    }
}
