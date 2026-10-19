# AYNEHA Calc - Application Android (Kotlin) - version 1.3.0

Calculatrice Android entièrement affichée en chiffres AYNEHA, l'écriture de la langue
songhay. Les nombres se lisent de droite à gauche, avec la virgule AYNEHA pour les décimales.
Une icône « i » sur l'accueil ouvre la page Confidentialité.

## Ouvrir le projet

1. Installez **Android Studio** (dernière version stable).
2. `Fichier > Ouvrir` puis sélectionnez le dossier du projet (`AynehaCalculator`).
3. Android Studio propose d'installer le wrapper Gradle manquant automatiquement à la
   première ouverture - acceptez.
4. Laissez la synchronisation Gradle se terminer (connexion Internet requise **uniquement
   pour compiler**, pas pour l'usage de l'application une fois installée).
5. Lancez sur un émulateur ou un téléphone via le bouton ▶ (Run) (`minSdk 24`).

## Ce qui est en place

- **Les 4 opérations** : addition, soustraction, multiplication (×) et division (÷),
  calculées avec `BigDecimal` pour éviter les erreurs d'arrondi.
- **Pourcentage (%)**, **virgule décimale**, **effacement d'un chiffre** et **effacement
  total (AC)**.
- **Ligne d'expression** au-dessus du résultat, affichée en plus petit.
- **Affichage RTL** : les chiffres AYNEHA sont inversés par points de code
  (`reversedByCodePoints`) pour se lire de droite à gauche.
- **Écran Confidentialité** (`PrivacyActivity`) : version, date de mise à jour et
  contact du développeur.
- **Police AYNEHA** embarquée (`res/font/ayneha_pro.ttf`) : aucune connexion requise
  pour l'afficher.
- Aucune permission Internet, aucun compte, aucun serveur.

## Version 1.3.0

- Migration des chiffres AYNEHA vers le bloc plan 16 (U+10FF7C - U+10FF85) et de la
  virgule AYNEHA (U+10FFDC), avec la police de transition `ayneha_pro.ttf`.
- Les chiffres sont des paires de substitution UTF-16 : la conversion utilise
  `Character.toChars` et l'inversion se fait par points de code, jamais par `char`.
- `versionCode` 4 / `versionName` 1.3.0.

## Structure du code

```
app/src/main/
├── java/com/maigus/aynehacalculator/
│   ├── MainActivity.kt        calculatrice (saisie, calcul, affichage RTL)
│   └── PrivacyActivity.kt     page Confidentialité
├── res/font/ayneha_pro.ttf    police AYNEHA
├── res/layout/                activity_main.xml, activity_privacy.xml
├── res/values/                textes, couleurs, thème
└── AndroidManifest.xml
```

## Licences

- **Code** : GNU GPL v3 ou ultérieure (fichier `LICENSE`), en-têtes SPDX
  `GPL-3.0-or-later`. Copyright (C) 2026 MAIGUS.
- **Police AYNEHA** : SIL Open Font License 1.1 (fichier `OFL.txt`), nom réservé « AYNEHA ».
- **Logos et images** : CC BY-SA 4.0. Le nom et le logo AYNEHA sont réservés.
- Développé par MAIGUS avec l'aide d'outils d'IA (Claude, Anthropic).

Logiciel libre (GPL v3). Code source : github.com/maigus223/ayneha-calculator

Site : https://ayneha.org - Contact : gwokmt2q@duck.com
