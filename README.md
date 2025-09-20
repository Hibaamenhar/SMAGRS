# SMAGRS - Système Multi-Agents de Gestion de Réservations de Salles

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![JADE](https://img.shields.io/badge/JADE-4.5.0-green.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)
![Status](https://img.shields.io/badge/Status-Completed-brightgreen.svg)

## 📋 Table des matières
- [Introduction](#introduction)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Installation](#installation)
- [Utilisation](#utilisation)
- [Diagrammes UML](#diagrammes-uml)
- [Auteurs](#auteurs)
- [Encadrement](#encadrement)
- [License](#license)

## 🎯 Introduction

SMAGRS est un système intelligent de gestion de réservations de salles basé sur une architecture multi-agents. Développé avec la plateforme JADE, il utilise le protocole FIPA Contract Net pour optimiser l'allocation des ressources et prévenir les conflits de réservation.

Ce projet a été réalisé dans le cadre du Master Intelligence Artificielle et Applications à la Faculté Polydisciplinaire de Ouarzazate, Université Ibn Zohr.

## ✨ Fonctionnalités

- **Réservation intelligente** : Allocation automatique basée sur des critères multiples
- **Gestion des conflits** : Prévention des doubles réservations
- **Interface intuitive** : Application Swing moderne avec thème FlatLaf
- **Négociation distribuée** : Protocole FIPA Contract Net entre agents
- **Sérialisation JSON** : Gestion avancée des données avec Jackson
- **Gestion temporelle** : Support des types Java Time (JSR-310)

## 🏗️ Architecture

### Architecture Multi-Agents
SMAGRS utilise trois types d'agents spécialisés :

1. **UIAgent** : Gère l'interface utilisateur et la communication O2A
2. **BrokerAgent** : Coordonne la négociation entre les agents
3. **RoomAgent** : Représente chaque salle et évalue les demandes

### Modèle de Données
- **Request** : Demande de réservation avec critères
- **Offer** : Proposition de réservation avec score
- **TimeSlot** : Gestion des créneaux horaires
- **Room** : Caractéristiques des salles

## 🛠️ Technologies

- **Java 17** : Langage de programmation
- **JADE 4.5.0** : Framework multi-agents
- **Swing** : Interface graphique
- **FlatLaf** : Thème moderne pour Swing
- **Jackson 2.17.0** : Sérialisation JSON
- **Maven** : Gestion des dépendances

## 📥 Installation

### Prérequis
- Java JDK 17 ou supérieur
- Maven 3.6 ou supérieur
- Git

### Clonage et compilation
```bash
# Cloner le dépôt
git clone https://github.com/Hibaamenhar/SMAGRS.git
cd SMAGRS

# Compiler avec Maven
mvn clean compile

# Exécuter l'application
mvn exec:java
