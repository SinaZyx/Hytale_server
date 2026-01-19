# HytaleFactions - Roadmap d'Amélioration

Ce document définit les améliorations planifiées pour le plugin HytaleFactions, basées sur les classes API Hytale disponibles dans `hytale_classes.txt`.

---

## État Actuel du Plugin

### Fonctionnalités Implémentées
- Gestion des factions (création, dissolution, renommage)
- Système de rôles (Leader, Officer, Recruit)
- Invitations et gestion des membres
- Claims de territoire (par chunk)
- Protection des blocs contre les non-membres (via `BreakBlockEvent` et `PlaceBlockEvent`)
- Système d'alliances et d'ennemis
- Home de faction et téléportation
- Mode chat faction (PUBLIC, FACTION, ALLY)
- Notifications d'entrée/sortie de territoire (avec couleurs selon relation)
- Vue frontières avec particules (`/f borders`)
- Particules lors du claim/unclaim
- Effets de conquête et victoire de guerre (particules + titres)
- Sons contextuels (invasion, guerre, roles, entree/sortie)
- Centre de notifications (historique + filtres)
- Pages UI (combat, guerre, perks, classement, evenements)
- Carte ASCII des claims (`/f map`)
- World map in-game (`/f map` 3 min, membres + allies + claims + overlay de chunk)
- Sauvegarde JSON avec backup automatique
- Integration FancyCore (permissions, placeholders, warps de home, economie)
- API publique + events (FactionsApi, bus d'evenements)
- **Statistiques de combat** (kills, deaths, KDR, streaks)
- **Système de guerre** (déclaration, points, reddition, durée limite)
- **Commandes combat** : `/f stats`, `/f top`, `/f war`

---

## Phase 1 - Combat & PvP ✅ COMPLÉTÉ

### 1.1 Tracking des Kills/Deaths ✅

**Objectif** : Suivre les statistiques de combat entre factions.

**Classes Hytale utilisées** :
```
com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent
```

**Fonctionnalités implémentées** :
- [x] Compteur de kills/deaths par membre (`MemberCombatStats.java`)
- [x] Statistiques de faction (total kills, K/D ratio) (`FactionCombatStats.java`)
- [x] Historique des derniers combats (lastKilled, lastKilledBy)
- [x] Commande `/f stats [player]` pour voir les statistiques
- [x] Commande `/f top [kills|kdr|factions]` pour les classements
- [x] Tracking des types de kills (NEUTRAL, FACTION, ALLY, ENEMY)
- [x] Kill streaks et best streak

**Fichiers créés** :
- `model/MemberCombatStats.java` - Statistiques de combat par joueur
- `model/FactionCombatStats.java` - Statistiques de combat par faction
- `storage/CombatDataStore.java` - Persistance JSON des données de combat
- `service/CombatService.java` - Logique métier combat et guerre

---

### 1.2 Système de Guerre ✅

**Objectif** : Permettre des guerres déclarées entre factions avec objectifs.

**Classes Hytale utilisées** :
```
com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent
com.hypixel.hytale.server.core.util.EventTitleUtil
```

**Fonctionnalités implémentées** :
- [x] Commande `/f war declare <faction>` - Déclaration de guerre
- [x] Commande `/f war status` - Statut de la guerre active
- [x] Commande `/f war surrender` - Reddition
- [x] Période de grâce configurable avant début de guerre
- [x] Points de guerre basés sur les kills
- [x] Conditions de victoire (points, temps, reddition)
- [x] Notifications de kills pendant la guerre
- [x] Historique des guerres avec limite configurable
- [x] Cooldown entre les guerres

**Configuration ajoutée dans `FactionSettings`** :
```java
public int warPointsPerKill = 10;
public int warPointsToWin = 100;
public int warGracePeriodMinutes = 5;
public int warDurationMinutes = 60;
public int warCooldownMinutes = 30;
public boolean warNotifyOnKill = true;
public String warKillMessage = "[Guerre] {killer} a tué {victim}!";
```

**Fichiers créés** :
- `model/War.java` - Modèle de guerre entre factions

---

### 1.3 Effets de Combat

**Objectif** : Ajouter des indicateurs visuels pendant les combats.

**Classes Hytale** :
```
com.hypixel.hytale.protocol.DamageEffects
com.hypixel.hytale.protocol.EntityEffect
com.hypixel.hytale.protocol.EntityEffectUpdate
com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect
com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent
com.hypixel.hytale.server.core.modules.entityui.asset.CombatTextUIComponent
```

**Fonctionnalités** :
- [ ] Texte de dégâts coloré selon l'affiliation (rouge ennemi, vert allié)
- [ ] Indicateur visuel au-dessus des joueurs ennemis
- [ ] Effets visuels lors d'un kill de faction

---

## Phase 2 - Effets Visuels & Particules ✅

### 2.1 Particules de Frontière ✅

**Objectif** : Visualiser les frontières des territoires avec des particules.

**Classes Hytale utilisées** :
```
com.hypixel.hytale.server.core.universe.world.ParticleUtil
com.hypixel.hytale.math.vector.Vector3d
```

**Fonctionnalités** :
- [x] Particules aux bordures de claim (toggle par joueur)
- [x] Couleur selon la faction propriétaire
- [x] Mode "vue frontière" temporaire (`/f borders`)
- [x] Particules lors du claim/unclaim

**Configuration** (config.json) :
```json
{
  "borderViewDurationSeconds": 15,
  "borderParticleIntervalSeconds": 1,
  "borderParticleStep": 4,
  "borderParticleCount": 1,
  "borderParticleHeightOffset": 1.2,
  "borderParticleOwn": "hytale:smoke",
  "borderParticleAlly": "hytale:smoke",
  "borderParticleEnemy": "hytale:smoke",
  "borderParticleNeutral": "hytale:smoke",
  "borderParticleWilderness": "hytale:smoke",
  "claimParticleAsset": "hytale:smoke",
  "unclaimParticleAsset": "hytale:smoke",
  "claimParticleCount": 20,
  "claimParticleHeightOffset": 1.2
}
```

---

### 2.2 Effets de Conquête ✅

**Objectif** : Effets visuels spectaculaires lors d'événements majeurs.

**Classes Hytale utilisées** :
```
com.hypixel.hytale.math.vector.Vector3d
com.hypixel.hytale.server.core.universe.world.ParticleUtil
com.hypixel.hytale.server.core.util.EventTitleUtil
```

**Fonctionnalités** :
- [x] Explosion de particules lors de la création de faction
- [x] Pilier de lumière lors d'un claim
- [x] Effet de conquête lors d'une prise de territoire pendant une guerre active
- [x] Célébration visuelle lors d'une victoire de guerre

**Configuration** (config.json) :
```json
{
  "factionCreateParticleAsset": "hytale:smoke",
  "factionCreateParticleCount": 40,
  "factionCreateParticleHeightOffset": 1.0,
  "factionCreateTitle": "Faction creee!",
  "factionCreateSubtitle": "{faction}",
  "factionCreateTitleFadeIn": 0.3,
  "factionCreateTitleStay": 3.0,
  "factionCreateTitleFadeOut": 0.5,
  "claimPillarParticleAsset": "hytale:smoke",
  "claimPillarHeight": 8,
  "claimPillarStep": 2,
  "claimPillarParticleCount": 2,
  "conquestParticleAsset": "hytale:smoke",
  "conquestParticleCount": 30,
  "conquestParticleHeightOffset": 1.2,
  "conquestTitle": "Conquete!",
  "conquestSubtitle": "{faction}",
  "conquestTitleFadeIn": 0.3,
  "conquestTitleStay": 2.5,
  "conquestTitleFadeOut": 0.5,
  "warVictoryParticleAsset": "hytale:smoke",
  "warVictoryParticleCount": 40,
  "warVictoryParticleHeightOffset": 1.2,
  "warVictoryTitle": "Victoire!",
  "warVictorySubtitle": "{faction}",
  "warVictoryTitleFadeIn": 0.3,
  "warVictoryTitleStay": 3.0,
  "warVictoryTitleFadeOut": 0.5
}
```

---

## Phase 3 - Audio & Immersion ✅

### 3.1 Effets Sonores

**Objectif** : Renforcer l'immersion avec des sons contextuels.

**Classes Hytale utilisées** :
```
com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
com.hypixel.hytale.server.core.universe.world.SoundUtil
com.hypixel.hytale.protocol.SoundCategory
```

**Fonctionnalités** :
- [x] Son d'alarme lors d'une invasion de territoire
- [x] Fanfare lors d'une victoire de guerre
- [x] Son de tambour lors d'une déclaration de guerre
- [x] Son d'entrée/sortie de territoire (optionnel)
- [x] Son de promotion/rétrogradation de membre

**Configuration** (config.json) :
```json
{
  "soundTerritoryInvasion": "hytale:alarm_bell",
  "soundWarVictory": "hytale:fanfare",
  "soundWarDeclare": "hytale:war_drums",
  "soundTerritoryEnter": "hytale:claim_sound",
  "soundTerritoryLeave": "hytale:claim_sound",
  "soundRolePromote": "hytale:claim_sound",
  "soundRoleDemote": "hytale:claim_sound",
  "soundVolume": 1.0,
  "soundPitch": 1.0,
  "soundInvasionCooldownSeconds": 10
}
```

---

## Phase 4 - Buffs & Effets de Statut

### 4.1 Bonus Territoriaux

**Objectif** : Avantages pour les joueurs sur leur territoire.

**Classes Hytale** :
```
com.hypixel.hytale.protocol.EntityEffect
com.hypixel.hytale.protocol.EntityEffectUpdate
com.hypixel.hytale.server.core.modules.entity.livingentity.LivingEntityEffectSystem
com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect
com.hypixel.hytale.protocol.Modifier
com.hypixel.hytale.protocol.ModifierTarget
com.hypixel.hytale.server.core.entity.StatModifiersManager
```

**Fonctionnalités** :
- [ ] Buff "Home Turf" sur son territoire (+10% dégâts, +5% vitesse)
- [ ] Régénération accélérée sur territoire allié
- [ ] Debuff pour les ennemis sur territoire hostile
- [ ] Bonus stackables selon le nombre de claims adjacents

**Modèle** :
```java
public class TerritoryBuff {
    private String effectId;
    private double damageBonus;
    private double speedBonus;
    private double regenBonus;
    private boolean requiresHomeTurf;
}
```

---

### 4.2 Perks de Faction

**Objectif** : Système de progression avec déblocage de capacités.

**Classes Hytale** :
```
com.hypixel.hytale.server.core.entity.StatModifiersManager
com.hypixel.hytale.builtin.adventure.reputation.assets.ReputationRank
```

**Fonctionnalités** :
- [ ] Niveaux de faction (basés sur activité, kills, claims)
- [ ] Perks débloquables (plus de claims, meilleurs buffs)
- [ ] Arbre de compétences faction
- [ ] Commande `/f perks` pour voir/activer les perks

**Perks Proposés** :
| Niveau | Perk | Effet |
|--------|------|-------|
| 1 | Fondation | +2 claims max |
| 2 | Fortification | -10% dégâts reçus sur territoire |
| 3 | Expansion | +1 claim par membre |
| 5 | Domination | Buff étendu aux territoires adjacents |
| 10 | Empire | Double points de guerre |

---

## Phase 5 - Notifications & UI ✅

### 5.1 Système de Notifications Avancé

**Objectif** : Notifications riches pour les événements importants.

**Classes Hytale utilisées** :
```
com.hypixel.hytale.protocol.packets.interface_.NotificationStyle
com.hypixel.hytale.server.core.util.NotificationUtil
com.hypixel.hytale.server.core.util.EventTitleUtil
```

**Fonctionnalités** :
- [x] Notification toast pour événements mineurs (membre connecté)
- [x] Notification plein écran pour événements majeurs (guerre déclarée)
- [x] Centre de notifications avec historique
- [x] Filtres de notification par type (`/f notify history <type>`)

**Types de Notifications** :
```java
public enum NotificationType {
    MINOR,
    MAJOR,
    WAR,
    TERRITORY,
    ROLE,
    SYSTEM
}
```

**Configuration** (config.json) :
```json
{
  "notificationHistoryLimit": 50
}
```

---

### 5.2 Amélioration du Menu UI

**Objectif** : Interface utilisateur plus riche et interactive.

**Classes Hytale** :
```
com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
com.hypixel.hytale.protocol.packets.interface_.Page
```

**Nouvelles Pages UI** :
- [x] Page de statistiques de combat
- [x] Page de gestion de guerre
- [x] Page de configuration des perks
- [x] Page de classement des factions
- [x] Page d'historique des événements

---

## Phase 6 - Économie de Faction

### 6.1 Trésor de Faction

**Objectif** : Ressources partagées entre membres.

**Classes Hytale** :
```
com.hypixel.hytale.protocol.ItemQuantity
com.hypixel.hytale.protocol.ItemWithAllMetadata
com.hypixel.hytale.protocol.packets.inventory.InventoryAction
com.hypixel.hytale.builtin.adventure.objectives.completion.GiveItemsCompletion
com.hypixel.hytale.builtin.adventure.shop.barter.BarterItemStack
```

**Fonctionnalités** :
- [ ] Coffre de faction virtuel (inventaire partagé)
- [ ] Commande `/f deposit <item> [amount]`
- [ ] Commande `/f withdraw <item> [amount]`
- [ ] Permissions par rôle (qui peut retirer)
- [ ] Historique des transactions

---

### 6.2 Taxation

**Objectif** : Système de revenus pour la faction.

**Fonctionnalités** :
- [ ] Taxe automatique sur les ressources collectées en territoire
- [ ] Taxe configurable par le leader
- [ ] Tribut des factions vassales
- [ ] Commande `/f tax set <percentage>`

---

## Phase 7 - Territoires Avancés

### 7.1 Zones Multi-Claims

**Objectif** : Gestion de territoires plus complexes.

**Classes Hytale** :
```
com.hypixel.hytale.server.worldgen.zone.Zone
com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent
com.hypixel.hytale.server.worldgen.zone.ZoneDiscoveryConfig
com.hypixel.hytale.server.worldgen.zone.ZoneColorMapping
```

**Fonctionnalités** :
- [ ] Régions nommées (groupes de claims)
- [ ] Zones avec règles spéciales (PvP, build, accès)
- [ ] Outposts (claims isolés avec bonus)
- [ ] Commande `/f region create <name>`

---

### 7.2 Points de Contrôle

**Objectif** : Objectifs capturables dans les territoires.

**Classes Hytale** :
```
com.hypixel.hytale.builtin.deployables.config.DeployableSpawner
com.hypixel.hytale.server.spawning.SpawningContext
```

**Fonctionnalités** :
- [ ] Balises capturables générant des points
- [ ] Temps de capture basé sur le nombre de joueurs
- [ ] Bonus pour le contrôle de plusieurs points
- [ ] Événements périodiques de conquête

---

## Phase 8 - Protection Avancée

### 8.1 Protection de Blocs Améliorée

**Objectif** : Système de protection plus granulaire.

**Classes Hytale** :
```
com.hypixel.hytale.protocol.BlockMount
com.hypixel.hytale.protocol.BlockMountType
com.hypixel.hytale.protocol.BlockFace
com.hypixel.hytale.protocol.BlockFaceSupport
com.hypixel.hytale.builtin.blockspawner.BlockSpawner
com.hypixel.hytale.protocol.BlockGathering
com.hypixel.hytale.builtin.buildertools.snapshot.BlockSelectionSnapshot
com.hypixel.hytale.builtin.blockphysics.BlockPhysicsPlugin
com.hypixel.hytale.builtin.blockphysics.BlockPhysicsUtil
```

**Fonctionnalités** :
- [ ] Protection par type de bloc (certains blocs protégés, d'autres non)
- [ ] Zones de construction autorisée pour alliés
- [ ] Système de rollback anti-grief
- [ ] Logs détaillés des modifications de blocs
- [ ] Protection contre les explosions configurable

**Configuration** :
```json
{
  "blockProtection": {
    "protectedBlocks": ["hytale:chest", "hytale:door"],
    "allowAllyBuild": true,
    "enableRollback": true,
    "rollbackHistoryMinutes": 60,
    "explosionProtection": true
  }
}
```

---

### 8.2 Système de Siège

**Objectif** : Permettre la conquête de territoire pendant les guerres.

**Fonctionnalités** :
- [ ] Mode siège activable pendant une guerre
- [ ] Protection réduite pendant le siège
- [ ] Canon de siège (destruction contrôlée)
- [ ] Temps limite pour le siège

---

## Phase 9 - Classement & Statistiques

### 9.1 Leaderboard des Factions

**Objectif** : Classement compétitif des factions.

**Classes Hytale** :
```
com.hypixel.hytale.protocol.EntityStatType
com.hypixel.hytale.protocol.EntityStatUpdate
com.hypixel.hytale.builtin.adventure.reputation.assets.ReputationRank
```

**Fonctionnalités** :
- [ ] Classement par puissance (claims × membres × kills)
- [ ] Classement par richesse (trésor)
- [ ] Classement par conquêtes (guerres gagnées)
- [ ] Commande `/f top [category]`
- [ ] Récompenses pour le top 3 mensuel

---

### 9.2 Historique et Logs

**Objectif** : Traçabilité complète des actions.

**Fonctionnalités** :
- [ ] Historique des guerres (résultats, participants)
- [ ] Historique des changements de territoire
- [ ] Timeline de la faction
- [ ] Export des statistiques

---

## Phase 10 - Intégrations ✅

### 10.1 Intégration FancyCore

**Objectif** : Tirer parti des fonctionnalités de FancyCore.

**Fonctionnalités** :
- [x] Permissions FancyCore pour les commandes faction
- [x] Économie FancyCore pour le trésor (bridge + balance faction)
- [x] Placeholders faction pour le chat (`faction_name`)
- [x] Intégration avec le système de homes (warp `faction_` + sync rename/disband)

---

### 10.2 API Externe

**Objectif** : Permettre l'intégration avec d'autres plugins.

**Fonctionnalités** :
- [x] API publique pour accéder aux données faction (`FactionsApi`)
- [x] Événements personnalisés (FactionCreatedEvent, WarStartedEvent, WarEndedEvent, etc.)
- [x] Hooks pour les systèmes externes (bus d'evenements + treasury)

---

## Résumé des Priorités

| Phase | Priorité | Effort | Impact |
|-------|----------|--------|--------|
| Phase 1 - Combat & PvP | Haute | Moyen | Très élevé |
| Phase 2 - Effets Visuels | Haute | Faible | Élevé |
| Phase 3 - Audio | Moyenne | Faible | Moyen |
| Phase 4 - Buffs | Moyenne | Moyen | Élevé |
| Phase 5 - Notifications | Moyenne | Moyen | Élevé |
| Phase 6 - Économie | Basse | Élevé | Moyen |
| Phase 7 - Territoires | Basse | Élevé | Élevé |
| Phase 8 - Protection | Moyenne | Moyen | Élevé |
| Phase 9 - Classement | Basse | Faible | Moyen |
| Phase 10 - Intégrations | Basse | Moyen | Moyen |

---

## Packages Hytale Principaux

```java
// Combat & Dégâts
import com.hypixel.hytale.protocol.DamageCause;
import com.hypixel.hytale.protocol.packets.player.DamageInfo;
import com.hypixel.hytale.protocol.packets.interface_.KillFeedMessage;
import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.DamageMemory;

// Particules
import com.hypixel.hytale.protocol.ParticleSystem;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;

// Effets Entités
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.StatModifiersManager;
import com.hypixel.hytale.protocol.EntityEffect;

// Sons
import com.hypixel.hytale.protocol.packets.world.PlaySoundEvent3D;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;

// Notifications
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.protocol.packets.interface_.Notification;

// Blocs
import com.hypixel.hytale.builtin.blockphysics.BlockPhysicsUtil;
import com.hypixel.hytale.builtin.buildertools.snapshot.BlockSelectionSnapshot;

// Zones
import com.hypixel.hytale.server.worldgen.zone.Zone;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
```

---

## Notes de Développement

1. **Respecter l'architecture Core/Adapter** - Toute nouvelle fonctionnalité doit séparer la logique métier (package principal) des adaptations Hytale (package `hytale/`).

2. **Vérifier les classes** - Avant d'utiliser une classe, vérifier sa présence dans `hytale_classes.txt`.

3. **Threading** - Utiliser `world.execute()` pour toute modification du monde.

4. **ECS** - Manipuler les entités via `Store<EntityStore>` et `Ref<EntityStore>`, jamais directement.

5. **Tests** - Tester chaque fonctionnalité en isolation avant intégration.
