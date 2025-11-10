import java.util.List;
import java.util.Map;
import java.util.Random;

public class MajCoursThread implements Runnable {

    private Map<Action, Integer> stockGlobal; 
    private List<Action> actions;              // Liste des actions
    private int intervalMillis;                // temps entre maj en ms
    private Random random;

    public MajCoursThread(List<Action> actions, Map<Action, Integer> stockGlobal, int intervalMillis){
        this.actions = actions;
        this.stockGlobal = stockGlobal;
        this.intervalMillis = intervalMillis;
        this.random = new Random();
    }

    public void run(){
        while (true){
            try{
                Thread.sleep(intervalMillis);

                synchronized (stockGlobal){
                    for (Action action : actions){
                        // Variation aléatoire -5% à +5%
                        double variation = (random.nextDouble() * 0.10) - 0.05;
                        double nouveauPrix = action.getPrix() * (1 + variation);

                        // Prix tjr positif
                        if (nouveauPrix <= 0) nouveauPrix = 0.01;

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
