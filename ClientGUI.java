import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map; 
import java.util.stream.Collectors; 

public class ClientGUI extends JFrame {

    private Client client;

    private JTextArea portefeuilleArea;
    private JPanel actionsPanel;

    // Attributs pour le Graphique
    private GraphiquePrix graphiquePanel; 
    private Action actionGraphiqueCourante = null;

    // COULEURS et STYLES POUR L'ESTHÉTIQUE (rendu public pour la classe interne GraphiquePrix)
    public static final Color BG_DARK = new Color(30, 30, 30); // Gris très foncé
    public static final Color FG_LIGHT = new Color(220, 220, 220); // Blanc cassé
    public static final Color ACCENT_GREEN = new Color(0, 180, 0); // Vert pour la finance
    public static final Color ACCENT_RED = new Color(255, 60, 60); // Rouge pour la perte
    private static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font FONT_TITLE = new Font("Arial", Font.BOLD, 16);


    public ClientGUI() {
        setTitle("Client Boursier - Marchés en Temps Réel");
        
        // Appliquer le look and feel pour un meilleur rendu
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) { /* Ignorer si non disponible */ }

        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);


        // --- Panneau création client (NORTH) ---
        JPanel connexionPanel = new JPanel(new GridLayout(3,2,5,5));
        connexionPanel.setBackground(BG_DARK);
        
        JTextField nomField = new JTextField();
        JTextField capitalField = new JTextField("1000");

        // Appliquer le style aux labels et champs de texte
        JLabel nomLabel = new JLabel("Nom :");
        nomLabel.setForeground(FG_LIGHT);
        JLabel capitalLabel = new JLabel("Capital :");
        capitalLabel.setForeground(FG_LIGHT);

        connexionPanel.add(nomLabel);
        connexionPanel.add(nomField);
        connexionPanel.add(capitalLabel);
        connexionPanel.add(capitalField);

        JButton creerBtn = new JButton("Créer client");
        JButton connecterBtn = new JButton("Se connecter au serveur");

        connexionPanel.add(creerBtn);
        connexionPanel.add(connecterBtn);

        connexionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(FG_LIGHT), 
                                                                "CONNEXION", 0, 0, FONT_TITLE, FG_LIGHT));
        add(connexionPanel, BorderLayout.NORTH);


        // --- Zone portefeuille (CENTER) ---
        portefeuilleArea = new JTextArea(10,30);
        portefeuilleArea.setEditable(false);
        portefeuilleArea.setFont(new Font("Monospaced", Font.BOLD, 24)); 
        portefeuilleArea.setBackground(BG_DARK);
        portefeuilleArea.setForeground(ACCENT_GREEN); 
        portefeuilleArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        
        JScrollPane scrollPortefeuille = new JScrollPane(portefeuilleArea);
        scrollPortefeuille.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(FG_LIGHT), 
                                                                "PORT. EN TEMPS RÉEL", 0, 0, FONT_TITLE, FG_LIGHT));
        add(scrollPortefeuille, BorderLayout.CENTER);


        // --- Zone actions (SOUTH) ---
        actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setBackground(BG_DARK);
        actionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(FG_LIGHT), 
                                                            "ACTIONS DU MARCHÉ", 0, 0, FONT_TITLE, FG_LIGHT));
        add(new JScrollPane(actionsPanel), BorderLayout.SOUTH);

        // --- NOUVELLE ZONE GRAPHIQUE (EAST) ---
        graphiquePanel = new GraphiquePrix();
        graphiquePanel.setBackground(BG_DARK.darker()); 
        graphiquePanel.setPreferredSize(new Dimension(400, 300)); 
        graphiquePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(FG_LIGHT), 
                                                            "HISTORIQUE (10 POINTS)", 0, 0, FONT_TITLE, FG_LIGHT));
        add(graphiquePanel, BorderLayout.EAST);
        
        // === Bouton création client ===
        creerBtn.addActionListener(e -> {
            String nom = nomField.getText();
            double capital;
            try {
                capital = Double.parseDouble(capitalField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Capital invalide !");
                return;
            }

            client = new Client(nom, capital);
            portefeuilleArea.setText(client.getPortefeuilleString());
            JOptionPane.showMessageDialog(this, "Client créé : " + nom);
        });

        // === Bouton connexion serveur ===
        connecterBtn.addActionListener(e -> {
            if (client == null) {
            JOptionPane.showMessageDialog(this, "Créez d'abord un client !");
            return;
        }

        try {
            client.seConnecter("localhost", 5001);
            JOptionPane.showMessageDialog(this, "Connexion réussie !");
            client.getActionsDisponibles(); // Première récupération
            afficherActions(); // Premier affichage
            
            // Lancement du thread de mise à jour
            client.lancerMiseAJourActions(this::afficherActions); 
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion : " + ex.getMessage());
        }
    });

        setVisible(true);
    }

    // Affichage dynamique des actions reçues du serveur
    private void afficherActions() {
        actionsPanel.removeAll();
        
        // Utilise le nouveau getter qui retourne la Map (Action -> Stock)
        Map<Action, Integer> stockMarche = client.getLastStockDisponible();
        
        if (stockMarche.isEmpty()) {
            actionsPanel.add(createHeaderLabel("EN ATTENTE DES DONNÉES DU MARCHÉ", FONT_TITLE, FG_LIGHT));
        }  

        // En-tête du panneau des actions avec les colonnes
        actionsPanel.add(createHeaderLabel(String.format("%-25s | %-10s | %-10s", 
                                                        "ACTION", "QTÉ STOCK", "PRIX ACTUEL"), // <-- CHANGEMENT DU TEXTE
                                                        FONT_TITLE, FG_LIGHT));
        actionsPanel.add(new JSeparator(SwingConstants.HORIZONTAL));


        // Recuperer dernières actions (prix les + recents)
        for (Map.Entry<Action, Integer> entry : stockMarche.entrySet()) { 
            Action a = entry.getKey();
            int quantiteDisponible = entry.getValue(); // <-- QUANTITÉ DU STOCK DU MARCHÉ

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setBackground(BG_DARK);

            // 2. Création de l'affichage (style colonne)
            String actionInfo = String.format("%-25s | %-10d | %.2f €", 
                                              a.getName(), 
                                              quantiteDisponible, 
                                              a.getPrix());

            JLabel label = new JLabel(actionInfo);
            label.setForeground(FG_LIGHT); 
            label.setFont(FONT_MONO); 
            label.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
            
            // Listener pour le graphique
            Action actionGraphique = a; // Variable effectivement finale pour le listener
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    actionGraphiqueCourante = actionGraphique; 
                    graphiquePanel.setHistorique(actionGraphique.getHistoriqueValeurs());
                    graphiquePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(FG_LIGHT), 
                                                            "HISTORIQUE DE " + actionGraphique.getName(), 0, 0, FONT_TITLE, FG_LIGHT));
                }
            });
            
            JButton acheterBtn = new JButton("Acheter 1");
            JButton vendreBtn = new JButton("Vendre 1");
            
            // Les Listeners appellent getPortefeuilleString() pour rafraîchir l'affichage
            Action actionPourListener = a; 
            
            acheterBtn.addActionListener(e -> {
                boolean marketUpdateNeeded = client.demanderAchat(actionPourListener, 1);
                if (marketUpdateNeeded) {
                    client.getActionsDisponibles(); // Mise à jour du marché si achat réussi
                }
                afficherActions(); 
            });

            vendreBtn.addActionListener(e -> {
                boolean marketUpdateNeeded = client.demanderVente(actionPourListener, 1);
                if (marketUpdateNeeded) {
                    client.getActionsDisponibles(); 
                }
                afficherActions(); 
            });

            panel.add(label);
            panel.add(acheterBtn);
            panel.add(vendreBtn);
            actionsPanel.add(panel);
        }
        
        // 3. Mise à jour de la zone du portefeuille
        if (client != null) {
            portefeuilleArea.setText(client.getPortefeuilleString()); 
        }
        
        // 4. Mise à jour en temps réel du graphique
        if (actionGraphiqueCourante != null) {
            // Utilise le keySet de la Map pour obtenir la liste des Actions mises à jour
            for (Action actionMiseAJour : stockMarche.keySet()) { // <-- FIX: Utilise stockMarche.keySet()
                if (actionMiseAJour.getName().equals(actionGraphiqueCourante.getName())) {
                    
                    // Mettre à jour l'objet mémorisé avec la nouvelle instance (et donc le nouvel historique)
                    actionGraphiqueCourante = actionMiseAJour;
                    
                    // Mettre à jour le panneau graphique avec le nouvel historique
                    graphiquePanel.setHistorique(actionGraphiqueCourante.getHistoriqueValeurs());
                    
                    // Mettre à jour la bordure
                    graphiquePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(FG_LIGHT), 
                                                            "HISTORIQUE DE " + actionGraphiqueCourante.getName(), 0, 0, FONT_TITLE, FG_LIGHT));
                    break;
                }
            }
        }

        actionsPanel.revalidate();
        actionsPanel.repaint();
    }
    
    // Méthode utilitaire pour les titres
    private JLabel createHeaderLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setBackground(BG_DARK);
        return label;
    }
    
}



//==============================================================
// CLASSE INTERNE 1 : Le panneau pour le graphique
// NOTE: Utilisez les constantes de couleur rendues publiques (ClientGUI.FG_LIGHT, etc.)
//==============================================================

class GraphiquePrix extends JPanel {
    
    private List<Double> historique;
    private static final int MARGIN = 30; // Marge pour les axes
    private static final int POINT_SIZE = 5;

    public void setHistorique(List<Double> historique) {
        // Garder seulement les 10 dernières valeurs
        this.historique = historique.size() > 10 ? historique.subList(historique.size() - 10, historique.size()) : historique;
        repaint(); // Force la redéfinition du graphique
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Dessiner le fond du panneau (pour s'assurer que BG_DARK.darker() est utilisé)
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (historique == null || historique.size() < 2) {
            g.setColor(ClientGUI.FG_LIGHT);
            g.drawString("Pas assez de données (min 2 points) ou non sélectionné.", MARGIN, getHeight() / 2);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        
        // Trouver le min et le max pour mettre à l'échelle
        double minPrix = historique.stream().min(Double::compare).get();
        double maxPrix = historique.stream().max(Double::compare).get();
        double range = maxPrix - minPrix;
        
        // --- Axes ---
        g.setColor(ClientGUI.FG_LIGHT); 
        g.drawLine(MARGIN, MARGIN, MARGIN, height - MARGIN); // Axe Y
        g.drawLine(MARGIN, height - MARGIN, width - MARGIN, height - MARGIN); // Axe X
        
        // Points et lignes
        int nPoints = historique.size();
        
        int prevX = 0;
        int prevY = 0;

        for (int i = 0; i < nPoints; i++) {
            double prix = historique.get(i);
            
            // Calcul de la position X
            int x = MARGIN + i * (width - 2 * MARGIN) / (nPoints - 1);
            
            // Calcul de la position Y (Mise à l'échelle)
            int y;
            if (range == 0) { 
                y = height - MARGIN - (height - 2 * MARGIN) / 2; 
            } else {
                y = height - MARGIN - (int) ((prix - minPrix) / range * (height - 2 * MARGIN));
            }
            
            // Dessiner la ligne entre les points
            if (i > 0) {
                g.setColor(ClientGUI.ACCENT_GREEN); 
                g.drawLine(prevX, prevY, x, y);
            }

            // Dessiner le point
            g.setColor(Color.CYAN);
            g.fillOval(x - POINT_SIZE / 2, y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
            
            // Afficher le prix
            g.setColor(ClientGUI.FG_LIGHT);
            g.drawString(String.format("%.2f", prix), x - 10, y - 10);
            
            // Mise à jour pour la prochaine itération
            prevX = x;
            prevY = y;
        }
    }
}