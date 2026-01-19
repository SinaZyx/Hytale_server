Hytale Factions (PvP)

Ce plugin ajoute des factions avec claims, alliances, UI, statistiques de combat et guerres.
Il est base sur le template officiel Hytale et cible Java 25.

Fonctionnalites
- Factions: creation, dissolution, roles, invitations, description/rename
- Claims de territoire (par chunk) + protection des blocs (BreakBlockEvent/PlaceBlockEvent)
- Alliances + ennemis
- Home de faction et teleports
- Modes de chat (PUBLIC, FACTION, ALLY)
- Notifications d'entree/sortie de territoire + historique
- Carte ASCII des claims (/f map)
- World map in-game (/f map, 3 min) avec membres/alliés + claims colores (overlay de chunk)
- Statistiques de combat (kills, deaths, KDR, streaks)
- Guerres: declaration, reddition, grace period, points, cooldown, historique
- UI: pages combat, guerre, classement, evenements, perks
- Effets visuels: borders, claim/unclaim, conquete, victoire de guerre
- Audio: invasion, entree/sortie de territoire, declaration/victoire de guerre, roles
- FancyCore: placeholders, permissions, warps de home, economie (treasury)

Commandes
- /f (ouvre le menu UI)
- /f menu
- /f create <name>
- /f disband
- /f list
- /f info [faction]
- /f who <player>
- /f map
- /f desc [texte]
- /f rename <name>
- /f invite <player>
- /f accept <faction>
- /f deny <faction>
- /f leave
- /f kick <player>
- /f promote <player>
- /f demote <player>
- /f leader <player>
- /f ally <faction>
- /f unally <faction>
- /f enemy <faction>
- /f unenemy <faction>
- /f sethome
- /f home
- /f claim
- /f unclaim
- /f chat
- /f borders [seconds]
- /f stats [player]
- /f top <kills|kdr|factions>
- /f war declare <faction>
- /f war surrender
- /f war status
- /f notify history [type] [limit]
- /f reload
- /f admin ...

Configuration (config.json)
- Base: minNameLength, maxNameLength, maxMembers, maxClaims
- Claims: chunkSize, baseClaimLimit, claimLimitPerMember, claimCooldownSeconds, unclaimCooldownSeconds
- Worlds: claimWorldAllowList, claimWorldDenyList
- Roles: roleForInvite, roleForKick, roleForClaim, roleForUnclaim, roleForSetHome, roleForWar, etc.
- Chat/notify: notifyOnEnter, notifyOnLeave, notifyUseTitle, notifyUseChat, notifyCooldownSeconds, notificationHistoryLimit
- Titles: claimEnterTitle, claimEnterSubtitle, claimLeaveTitle, claimLeaveSubtitle, claimTitleFadeIn, claimTitleStay, claimTitleFadeOut
- World map: worldMapDurationSeconds, worldMapUpdateIntervalSeconds
- Colors: colorOwn, colorAlly, colorEnemy, colorNeutral, colorWilderness
- War: warPointsPerKill, warPointsToWin, warGracePeriodMinutes, warDurationMinutes, warCooldownMinutes, warNotifyOnKill, warKillMessage
- Particules: borderParticle*, claimParticle*, factionCreateParticle*, conquestParticle*, warVictoryParticle*
- Sons: soundTerritoryInvasion, soundWarVictory, soundWarDeclare, soundTerritoryEnter, soundTerritoryLeave,
        soundRolePromote, soundRoleDemote, soundVolume, soundPitch, soundInvasionCooldownSeconds

Donnees et fichiers
- data/factions.json (inclut treasuryBalance par faction)
- data/combat.json (stats combat + guerres)
- data/logs/actions.log (actions)

Integrations FancyCore
- Placeholders: {faction_name}
- Permissions: check via FancyCore PermissionService (fallback Hytale)
- Home: creation d'un warp FancyCore pour le home de faction (prefix: faction_)
- Economie: bridge via HytaleFactionsPlugin.depositTreasury / withdrawTreasury

API publique + events
- Acces via HytaleFactionsPlugin.api() (FactionsApi)
- Events: FactionCreatedEvent, FactionRenamedEvent, FactionDisbandedEvent, FactionHomeSetEvent,
          FactionClaimChangedEvent, MemberRoleChangedEvent, WarDeclaredEvent, WarStartedEvent,
          WarEndedEvent, FactionTreasuryChangedEvent

Installation
1) Installer Java 25.
2) Ajuster les valeurs dans gradle.properties si besoin.
3) Mettre a jour src/main/resources/manifest.json avec tes infos.
4) Ouvrir le projet dans IntelliJ IDEA et laisser Gradle synchroniser.
5) Lancer la configuration HytaleServer generee par le template.

Reprendre le projet
1) Ouvre le projet dans IntelliJ IDEA.
2) Verifie JAVA_HOME (Java 25) et que Gradle est bien synchronise.
3) Regarde gradle.properties (includes_pack=true, load_user_mods=true si tu lances depuis l'IDE).
4) Build local: .\gradlew.bat build

Tester en local avant serveur
Option A (via IntelliJ)
1) Dans gradle.properties: load_user_mods=true.
2) Lance la configuration "HytaleServer" generee par le template.
3) Connecte-toi en local et teste /f.

Option B (jar dans Mods)
1) Build: .\gradlew.bat build
2) Copie build\libs\HytaleFactions-0.2.0.jar vers %appdata%\Hytale\UserData\Mods
3) Demarre le serveur puis le client, et teste /f.
