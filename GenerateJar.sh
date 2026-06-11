#!/bin/bash

# 1. Définition des variables
APP_NAME="MonFramework"
SRC_DIR="src/java"
BUILD_DIR="build"
LIB_DIR="lib"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"

# 2. Nettoyage et création du dossier de build
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/classes

# 3. Liste et compilation des fichiers Java du framework
find $SRC_DIR -name "*.java" > sources.txt
javac -cp $SERVLET_API_JAR -d $BUILD_DIR/classes @sources.txt
rm sources.txt

# 4. Génération du fichier .jar du framework
jar -cvf $APP_NAME.jar -C $BUILD_DIR/classes .

echo "Le JAR du framework a été généré avec succès !"