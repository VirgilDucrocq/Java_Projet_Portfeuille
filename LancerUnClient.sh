#!/bin/bash

# 1. Compilation
echo "Étape 1 : Compilation des fichiers Java"
# Suppression des anciens fichiers .class
rm -f *.class
javac *.java

# Vérification compilation réussi
if [ $? -ne 0 ]; then
    echo "Erreur de compilation, le script s'arrête"
    exit 1
fi
echo "Compilation réussie."
echo "---"

# 2. Lancement du Serveur en arrière-plan
echo "Étape 2 : Lancement du Serveur en arrière-plan"
java Serveur &
SERVER_PID=$!
echo "Le Serveur a été lancé avec le PID : $SERVER_PID"

# Petite pause pour s'assurer que le serveur est bien démarré
sleep 2

# 3. Lancement du Client
echo "Étape 3 : Lancement du Client"
java Client
echo "Le Client a terminé son exécution."
kill $SERVER_PID
rm -f *.class

# ps aux | grep java | grep Serveur
# et kill <PID> (le deuxième nombre) si jamais un serveur n'a pas été tué avant et tourne derrière
