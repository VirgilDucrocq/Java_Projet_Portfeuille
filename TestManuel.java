import java.util.*;

public class TestManuel {

    // --- Helpers pour les tests ---

    public static void ok(boolean condition, String test) {
        System.out.println((condition ? "✅ " : "❌ ") + test);
    }

    public static void assertVal(double attendu, double recu, String test) {
        boolean succes = Math.abs(attendu - recu) < 0.01;
        System.out.println((succes ? "✅ " : "❌ ") + test + (succes ? "" : " (Attendu: " + attendu + ", Reçu: " + recu + ")"));
    }

    // --- Main ---

    public static void main(String[] args) {
        System.out.println("--- Lancement des tests ---");

        testActionEquals();
        testHistorique();
        testPRUSimple();
        testPRUComplexe();
        testVente();
        testRobustesse();
        testMaths();

        System.out.println("\n--- Fin des tests ---");
    }

    // --- Tests ---

    private static void testActionEquals() {
        System.out.println("\n[1] Test Equals & HashCode");
        try {
            // Même nom = même action pour la Map, peu importe le prix
            Action a1 = new Action("Tesla", 100, ActionType.VALEUR_TECH_CLASSIQUE);
            Action a2 = new Action("Tesla", 200, ActionType.VALEUR_TECH_CLASSIQUE);
            Action a3 = new Action("Apple", 100, ActionType.VALEUR_TECH_CLASSIQUE);

            ok(a1.equals(a2), "Tesla == Tesla (prix diff)");
            ok(!a1.equals(a3), "Tesla != Apple");
            ok(a1.hashCode() == a2.hashCode(), "HashCode identique");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void testHistorique() {
        System.out.println("\n[2] Test Historique (FIFO)");
        try {
            Action google = new Action("Google", 100, ActionType.INDICE_STABLE);
            
            // On sature l'historique (max 20)
            for (int i = 1; i <= 30; i++) google.setPrix(100 + i);

            ok(google.getHistoriqueValeurs().size() == 20, "Taille plafonnée à 20");
            
            double dernier = google.getHistoriqueValeurs().get(google.getHistoriqueValeurs().size() - 1);
            assertVal(130.0, dernier, "Dernière valeur conservée");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void testPRUSimple() {
        System.out.println("\n[3] Test PRU Simple");
        try {
            Client c = new Client("Test", 5000);
            Portefeuille p = c.getPortefeuille();
            
            // Achat 10 à 100€ puis 10 à 150€ -> Moyenne 125€
            p.ajouterAction(new Action("Apple", 100, ActionType.VALEUR_TECH_CLASSIQUE), 10);
            p.ajouterAction(new Action("Apple", 150, ActionType.VALEUR_TECH_CLASSIQUE), 10);
            
            Action a = getAction(p, "Apple");
            assertVal(125.0, a.getPrix(), "PRU calculé correct");
            assertVal(2500.0, p.getMontantTotalInvesti(), "Total investi correct");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void testPRUComplexe() {
        System.out.println("\n[4] Test PRU Pondéré");
        try {
            Portefeuille p = new Client("Test", 5000).getPortefeuille();
            
            // 10 actions à 100€ (1000€) + 5 actions à 200€ (1000€)
            // Total 2000€ pour 15 actions -> 133.33€
            p.ajouterAction(new Action("MSFT", 100, ActionType.INDICE_STABLE), 10);
            p.ajouterAction(new Action("MSFT", 200, ActionType.INDICE_STABLE), 5);
            
            Action a = getAction(p, "MSFT");
            ok(p.getPortefeuille().get(a) == 15, "Quantité totale OK");
            assertVal(133.33, a.getPrix(), "PRU pondéré OK");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void testVente() {
        System.out.println("\n[5] Test Vente/Retrait");
        try {
            Portefeuille p = new Client("Test", 5000).getPortefeuille();
            Action total = new Action("Total", 50, ActionType.MATIERE_PREMIERE_INDUSTRIELLE);
            
            p.ajouterAction(total, 20);
            p.retirerAction(total, 15); // Reste 5
            
            Action a = getAction(p, "Total");
            ok(p.getPortefeuille().get(a) == 5, "Reste 5 après vente");
            
            p.retirerAction(total, 5); // Reste 0
            ok(!p.getPortefeuille().containsKey(total), "Suppression de la map si 0");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void testRobustesse() {
        System.out.println("\n[6] Test Robustesse (Exceptions)");
        try {
            Portefeuille p = new Client("Test", 1000).getPortefeuille();
            Action btc = new Action("BTC", 20000, ActionType.ACTION_VOLATILE_CRYPTO);
            p.ajouterAction(btc, 1);

            try {
                p.retirerAction(btc, 5); // On en a 1, on veut en vendre 5
                ok(false, "Aurait dû planter");
            } catch (IllegalArgumentException e) {
                ok(true, "Exception levée (Vente excessive refusée)");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void testMaths() {
        System.out.println("\n[7] Test Modèle Maths");
        try {
            MajCoursThread math = new MajCoursThread(new Serveur(0, new HashMap<>()), 1000);
            double prix = math.calculerNouveauPrix(100.0, 0.05, 0.20, 1.0/365.0);
            
            ok(prix > 0 && !Double.isInfinite(prix), "Prix généré cohérent (" + String.format("%.2f", prix) + ")");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static Action getAction(Portefeuille p, String nom) {
        for(Action a : p.getPortefeuille().keySet()) {
            if (a.getNom().equals(nom)) return a;
        }
        return null;
    }
}