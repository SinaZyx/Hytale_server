# HytaleDuels - Plugin de Duels 1v1/2v2

Plugin complet de duels pour Hytale avec système de kits, matchmaking, arènes et classement ELO.

---

## Table des matières

1. [Fonctionnalités](#fonctionnalités)
2. [Architecture du projet](#architecture-du-projet)
3. [Commandes](#commandes)
4. [Système de Kits](#système-de-kits)
5. [Système de Matchmaking](#système-de-matchmaking)
6. [Système de Ranking (ELO)](#système-de-ranking-elo)
7. [Interfaces UI](#interfaces-ui)
8. [Configuration](#configuration)
9. [Installation](#installation)
10. [Améliorations possibles](#améliorations-possibles)

---

## Fonctionnalités

### Implémenté

- **Duels 1v1 et 2v2** avec système d'invitation et file d'attente
- **Kits personnalisables** (armure, items, effets)
- **Arènes multiples** avec spawns configurables
- **Système ELO complet** avec 18 rangs
- **Leaderboard** triable par ELO, victoires, winrate
- **UI Admin** pour gérer kits, arènes et matchs
- **UI Ranking** pour voir classement et stats
- **Persistance JSON** de toutes les données

### Non implémenté (TODO)

- Event listener de mort (détection automatique fin de match)
- Téléportation retour au lobby après match
- Countdown avant début de match
- Édition complète des items/armure dans l'UI admin

---

## Architecture du projet

```
hytale-duels/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── src/main/
│   ├── resources/
│   │   ├── manifest.json
│   │   └── Pages/
│   │       ├── DuelsAdmin.ui      # UI admin
│   │       └── RankingMenu.ui     # UI classement
│   └── java/com/kingc/hytale/duels/
│       ├── DuelsPlugin.java       # Point d'entrée core
│       ├── api/                   # Abstractions (indépendant de Hytale)
│       │   ├── PlayerRef.java
│       │   ├── Location.java
│       │   ├── CommandSource.java
│       │   ├── ServerAdapter.java
│       │   └── ItemStack.java
│       ├── kit/                   # Système de kits
│       │   ├── KitDefinition.java
│       │   ├── KitRepository.java
│       │   └── KitService.java
│       ├── arena/                 # Gestion des arènes
│       │   ├── Arena.java
│       │   ├── ArenaRepository.java
│       │   └── ArenaService.java
│       ├── match/                 # Matchmaking & duels
│       │   ├── DuelRequest.java
│       │   ├── Match.java
│       │   ├── MatchState.java
│       │   ├── MatchType.java
│       │   └── MatchService.java
│       ├── queue/                 # Files d'attente
│       │   ├── QueueEntry.java
│       │   └── QueueService.java
│       ├── ranking/               # Système ELO
│       │   ├── PlayerStats.java
│       │   ├── Rank.java
│       │   ├── EloCalculator.java
│       │   ├── RankingRepository.java
│       │   └── RankingService.java
│       ├── command/               # Commandes
│       │   └── CommandDispatcher.java
│       └── hytale/                # Adaptation Hytale
│           ├── HytaleDuelsPlugin.java
│           ├── HytalePlayerRef.java
│           ├── HytaleCommandSource.java
│           ├── HytaleServerAdapter.java
│           ├── DuelsAdminPage.java
│           └── RankingMenuPage.java
```

### Principes d'architecture

1. **Séparation core/adapter** : Le code métier (`DuelsPlugin`, services) est indépendant de Hytale. Seul le package `hytale/` contient les adaptations spécifiques.

2. **Repository pattern** : Chaque entité (Kit, Arena, PlayerStats) a son repository pour la persistance JSON.

3. **Service layer** : La logique métier est dans les services, pas dans les commandes ou l'UI.

---

## Commandes

### Duels

| Commande | Description |
|----------|-------------|
| `/duel <joueur> [kit]` | Défier un joueur (kit par défaut: tank) |
| `/duel accept` | Accepter un défi |
| `/duel decline` | Refuser un défi |

### File d'attente

| Commande | Description |
|----------|-------------|
| `/queue 1v1 [kit]` | Rejoindre la file 1v1 |
| `/queue 2v2 [kit]` | Rejoindre la file 2v2 |
| `/queue leave` | Quitter la file |
| `/queue status` | Voir son statut |

### Kits

| Commande | Description |
|----------|-------------|
| `/kit list` | Lister les kits disponibles |
| `/kit info <nom>` | Détails d'un kit |
| `/kit preview <nom>` | Essayer un kit (reçoit l'équipement) |

### Ranking

| Commande | Description |
|----------|-------------|
| `/stats` | Voir ses propres stats |
| `/stats <joueur>` | Voir les stats d'un joueur |
| `/top` | Top 10 par ELO |
| `/top wins` | Top 10 par victoires |
| `/top winrate` | Top 10 par winrate |
| `/ranking` | **Ouvre le menu UI du classement** |

### Administration

| Commande | Description |
|----------|-------------|
| `/duelsadmin` | Ouvre le panel admin (kits, arènes, matchs) |

---

## Système de Kits

### Structure d'un kit

```java
KitDefinition {
    String id;              // Identifiant unique (ex: "archer")
    String displayName;     // Nom affiché (ex: "Archer")
    String iconItem;        // Item pour l'icône (ex: "hytale:bow")
    ItemStack helmet;       // Casque
    ItemStack chestplate;   // Plastron
    ItemStack leggings;     // Jambières
    ItemStack boots;        // Bottes
    List<ItemStack> items;  // Items de l'inventaire
    Map<String, EffectEntry> effects;  // Effets (force, speed, etc.)
}
```

### Kits par défaut

| Kit | Description |
|-----|-------------|
| **Archer** | Armure cuir, arc, 64 flèches, épée pierre |
| **Tank** | Armure fer, épée fer, bouclier |
| **Berserker** | Armure chaîne, hache diamant, effet force |

### Fichier de configuration

Les kits sont sauvegardés dans `data/kits.json` et peuvent être édités manuellement ou via `/duelsadmin`.

---

## Système de Matchmaking

### Types de matchs

| Type | Joueurs | Description |
|------|---------|-------------|
| `DUEL_1V1` | 2 | Duel classique |
| `DUEL_2V2` | 4 | Équipes de 2 |

### Flux d'un match

1. **Invitation** (`/duel <joueur>`) ou **Queue** (`/queue 1v1`)
2. **Acceptation** ou **Match trouvé** dans la queue
3. **Réservation** d'une arène disponible
4. **Téléportation** aux spawns + **Application** du kit
5. **Combat** jusqu'à la mort d'un joueur
6. **Fin** : calcul ELO, libération de l'arène

### Règles

- Un défi expire après **30 secondes**
- Cooldown de **5 secondes** entre les invitations
- Un joueur ne peut être que dans **1 match** ou **1 queue** à la fois

---

## Système de Ranking (ELO)

### Les 18 rangs

| Rang | ELO | Couleur |
|------|-----|---------|
| Bronze III | 0-799 | 🟤 `#cd7f32` |
| Bronze II | 800-899 | 🟤 |
| Bronze I | 900-999 | 🟤 |
| Silver III | 1000-1099 | ⚪ `#c0c0c0` |
| Silver II | 1100-1199 | ⚪ |
| Silver I | 1200-1299 | ⚪ |
| Gold III | 1300-1399 | 🟡 `#ffd700` |
| Gold II | 1400-1499 | 🟡 |
| Gold I | 1500-1599 | 🟡 |
| Platinum III | 1600-1699 | 🔵 `#00cec9` |
| Platinum II | 1700-1799 | 🔵 |
| Platinum I | 1800-1899 | 🔵 |
| Diamond III | 1900-1999 | 💎 `#74b9ff` |
| Diamond II | 2000-2099 | 💎 |
| Diamond I | 2100-2199 | 💎 |
| Master | 2200-2399 | 🟣 `#a29bfe` |
| Grandmaster | 2400-2599 | 💗 `#fd79a8` |
| Champion | 2600+ | 👑 `#e84393` |

### Calcul ELO

Le système utilise la formule ELO standard avec des ajustements :

```
Gain = K × (1 - Probabilité_victoire_attendue) + Bonus
```

#### Facteur K (amplitude des changements)

| Condition | K |
|-----------|---|
| Nouveau joueur (< 30 matchs) | 40 |
| Joueur normal | 32 |
| Vétéran (> 100 matchs) | 24 |
| Élite (> 2200 ELO) | 16 |

#### Bonus

| Bonus | Valeur | Condition |
|-------|--------|-----------|
| Win streak | +5/victoire | Max 3 victoires consécutives |
| Upset | +10 | Victoire contre +200 ELO |
| Upset partiel | +5 | Victoire contre +100 ELO |

#### Exemple

- Joueur A (1200 ELO) bat Joueur B (1400 ELO)
- Probabilité attendue de A : ~24%
- Gain de base : 32 × (1 - 0.24) = ~24
- Bonus upset : +10
- **Gain total A : +34 ELO**
- **Perte B : -24 ELO**

### Stats trackées

- ELO actuel
- Victoires / Défaites
- Winrate (%)
- Série de victoires actuelle
- Meilleure série de victoires
- Date du dernier match

---

## Interfaces UI

### Menu Ranking (`/ranking`)

3 onglets :
1. **Classement** : Top 20 avec tri (ELO, victoires, winrate)
2. **Mes Stats** : Stats détaillées + progression vers prochain rang
3. **Rangs** : Liste de tous les rangs avec ELO requis

### Panel Admin (`/duelsadmin`)

3 onglets :
1. **Kits** : Créer, modifier (nom, icône), supprimer
2. **Arènes** : Créer, définir spawns T1/T2, supprimer
3. **Matchs** : Voir matchs en cours, terminer manuellement

---

## Configuration

### Fichiers générés

| Fichier | Contenu |
|---------|---------|
| `data/kits.json` | Définitions des kits |
| `data/arenas.json` | Définitions des arènes |
| `data/rankings.json` | Stats des joueurs |

### Exemple de kit (JSON)

```json
{
  "archer": {
    "id": "archer",
    "displayName": "Archer",
    "iconItem": "hytale:bow",
    "helmet": { "itemId": "hytale:leather_helmet", "count": 1 },
    "chestplate": { "itemId": "hytale:leather_chestplate", "count": 1 },
    "leggings": { "itemId": "hytale:leather_leggings", "count": 1 },
    "boots": { "itemId": "hytale:leather_boots", "count": 1 },
    "items": [
      { "itemId": "hytale:bow", "count": 1 },
      { "itemId": "hytale:arrow", "count": 64 },
      { "itemId": "hytale:stone_sword", "count": 1 }
    ],
    "effects": {}
  }
}
```

### Exemple d'arène (JSON)

```json
{
  "arena1": {
    "id": "arena1",
    "displayName": "Arena 1",
    "team1Spawns": [{ "world": "world", "x": 0, "y": 64, "z": 10 }],
    "team2Spawns": [{ "world": "world", "x": 0, "y": 64, "z": -10 }],
    "spectatorSpawn": { "world": "world", "x": 20, "y": 70, "z": 0 },
    "maxPlayers": 2
  }
}
```

---

## Installation

### Prérequis

- Hytale installé via le launcher officiel
- Java 25 (inclus avec Hytale)
- Gradle (inclus via wrapper)

### Compilation

```bash
cd C:\Users\kingc\hytale-workspace\hytale-duels
.\gradlew build
```

### Déploiement

Le `.jar` généré se trouve dans `build/libs/`. Copier vers le dossier plugins du serveur Hytale.

---

## Améliorations possibles

### Priorité haute

| Amélioration | Description | Complexité |
|--------------|-------------|------------|
| **Event listener de mort** | Détecter automatiquement la fin d'un match quand un joueur meurt | Moyenne |
| **Téléportation lobby** | Renvoyer les joueurs au spawn après un match | Facile |
| **Countdown** | Afficher 3, 2, 1 avant le début du match | Facile |
| **Vérification API Hytale** | Adapter `HytaleServerAdapter` à l'API réelle (items, effets, téléport) | Moyenne |

### Priorité moyenne

| Amélioration | Description | Complexité |
|--------------|-------------|------------|
| **Édition complète des kits** | UI pour modifier armure et items (pas juste nom/icône) | Haute |
| **Système de permissions** | Vérifier les permissions admin pour `/duelsadmin` | Facile |
| **Spectateur mode** | Permettre de regarder un match en cours | Moyenne |
| **Rematch** | Bouton pour relancer un duel après un match | Facile |
| **Historique des matchs** | Sauvegarder et afficher l'historique | Moyenne |

### Priorité basse (features avancées)

| Amélioration | Description | Complexité |
|--------------|-------------|------------|
| **Tournois** | Système de brackets automatique | Haute |
| **Saisons** | Reset ELO périodique avec récompenses | Moyenne |
| **Récompenses** | Donner des items/cosmétiques selon le rang | Moyenne |
| **Matchmaking par ELO** | Queue qui match par niveau similaire | Moyenne |
| **Anti-leave** | Pénalité ELO si déconnexion en match | Facile |
| **Ranked vs Unranked** | Séparer les modes de jeu | Moyenne |
| **Statistiques avancées** | K/D ratio, dégâts infligés, temps moyen de match | Haute |
| **Replays** | Enregistrer et rejouer les matchs | Très haute |
| **API externe** | Endpoint HTTP pour afficher les stats sur un site web | Moyenne |
| **Discord integration** | Webhook pour annoncer les matchs/promotions | Facile |

### Optimisations techniques

| Amélioration | Description |
|--------------|-------------|
| **Cache leaderboard** | Éviter de recalculer le top à chaque requête |
| **Sauvegarde async** | Sauvegarder les données en arrière-plan |
| **Base de données** | Remplacer JSON par SQLite/MySQL pour gros serveurs |
| **Tests unitaires** | Ajouter des tests pour EloCalculator, services |

---

## Crédits

Développé pour Hytale par kingc.

**Structure basée sur** : Pattern similaire au plugin HytaleFactions avec séparation core/adapter.

---

## Changelog

### v0.1.0 (Initial)

- Système de duels 1v1 et 2v2
- 3 kits par défaut (Archer, Tank, Berserker)
- File d'attente avec matchmaking
- Système ELO complet avec 18 rangs
- UI Admin pour gestion kits/arènes
- UI Ranking avec leaderboard
- Commandes : /duel, /queue, /kit, /stats, /top, /ranking, /duelsadmin
