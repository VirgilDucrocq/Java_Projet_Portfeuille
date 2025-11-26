import java.util.List;
import java.util.Map;
import java.util.Random;

//Classe qui va gérer le thread de mise à jour des cours -> doit obligatoirement implémenter runnable donc
public class MajCoursThread implements Runnable{

    private Map<Action, Integer> stockGlobal;
    private List<Action> actions;              // Liste des actions
    private int intervalleMillis;              // temps entre maj en ms
    private Random generateurAleatoire;         // Besoin d'aléatoire

    //Constructeur
    public MajCoursThread(List<Action> actions, Map<Action, Integer> stockGlobal, int intervalleMillis){
        this.actions = actions;
        this.stockGlobal = stockGlobal;
        this.intervalleMillis = intervalleMillis;
        this.generateurAleatoire = new Random();
    }

    //Calcul du prix à l'aide du modèle
    public double calculerNouveauPrix(double prixActuel, double mu, double sigma, double dtAnnee){
    double dt = dtAnnee;

    double termeStochastique = sigma * Math.sqrt(dt) * generateurAleatoire.nextGaussian();
    double termeDerive = (mu - (sigma * sigma) / 2.0) * dt;

    double nouveauPrix = prixActuel * Math.exp(termeDerive + termeStochastique);

    return nouveauPrix;
}

    //run pour le thread 
    public void run(){
        // Conversion de l'intervalle de temps en secondes (dt) 
        final double SECONDES_PAR_AN = 365.25;
        final double dtAnnee = (intervalleMillis / 1000.0) / SECONDES_PAR_AN;

        while (true){
            try{
                Thread.sleep(intervalleMillis);

                synchronized (stockGlobal){
                    for (Action action : actions){
                        double mu = action.getMu(); // Taux de croissance annuel espéré
                        double sigma = action.getSigma(); // Volatilité annuelle espérée

                        double nouveauPrix = Math.floor(100*calculerNouveauPrix(action.getPrix(), mu, sigma, dtAnnee)) / 100.0;

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
