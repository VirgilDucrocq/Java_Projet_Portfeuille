#!/bin/bash

#Variables et Fonctions de Nettoyage

SERVER_PID=""
CLIENTS_DIR="clients" # Dossier des sauvegardes clients
SERVER_HIST_BIN="historique_binaire.ser" # Fichier d'historique binaire du Serveur
SERVER_TRANS_TXT="transactions_lisibles.txt" # Fichier d'historique lisible du Serveur

# Fonction pour s'assurer que le serveur est tué à la fin
cleanup() {
    echo ""
    echo "Début du Nettoyage"
    
    # Arrêt du Serveur
    if [ -n "$SERVER_PID" ]; then
        echo "Arrêt du Serveur (PID : $SERVER_PID)"
        kill -15 "$SERVER_PID" 2>/dev/null
        sleep 2
        kill -9 "$SERVER_PID" 2>/dev/null
    fi
    
    #Nettoyage du dossier des sauvegardes clients (fichiers .ser)
    if [ -d "$CLIENTS_DIR" ]; then
        echo "Nettoyage des sauvegardes clients dans le dossier '$CLIENTS_DIR'"
        rm -rf "$CLIENTS_DIR"/*
    fi
    
    # Suppression des fichiers d'historique du Serveur
    echo "Suppression des fichiers d'historique du Serveur..."
    rm -f "$SERVER_HIST_BIN"
    rm -f "$SERVER_TRANS_TXT"
    
    # Nettoyage des fichiers compilés (.class)
    echo "Suppression des fichiers .class..."
    rm -f *.class
    
    echo "Nettoyage Terminé"
}

# Piège pour exécuter la fonction cleanup à la sortie du script
trap cleanup EXIT

# Fonction pour afficher le menu
afficher_menu() {
    echo ""
    echo "--- Menu de Gestion Serveur/Client ---"
    echo "1) Lancer un nouveau Client (en arrière-plan)"
    echo "2) Afficher l'état (processus Serveur/Clients Java)"
    echo "3) Arrêter et Quitter (tue le Serveur)"
    echo "---------------------------------------"
    echo -n "Votre choix : "
}

#Compilation

echo "Étape 1 : Compilation des fichiers Java"
# Suppression des anciens fichiers .class
rm -f *.class
javac *.java

#Lancement du Serveur en arrière-plan

echo "Étape 2 : Lancement du Serveur en arrière-plan"
# Lance le serveur et stocke son PID
java Serveur &
SERVER_PID=$!
echo "Le Serveur a été lancé avec le PID : $SERVER_PID"

# Petite pause pour s'assurer que le serveur est bien démarré
sleep 2

# Boucle du Menu Principal

while true; do
    afficher_menu
    read -r choix

    case $choix in
        1)
            echo "Lancement d'un nouveau Client (arrière-plan)"
            # Lancement en arrière-plan
            java Client &
            CLIENT_PID=$!
            echo "Client lancé avec le PID : $CLIENT_PID"
            ;;
        2)
            echo "État des processus Java en cours"
            # Affiche les processus java, y compris le serveur et les clients
            ps aux | grep java | grep -E 'Serveur|Client' | grep -v grep
            ;;
        3)
            echo "Arrêt demandé, exécution du nettoyage et sortie"
            exit 0 # Le trap EXIT va appeler cleanup()
            ;;
        *)
            echo "Choix invalide, veuillez réessayer svp"
            ;;
    esac
done
