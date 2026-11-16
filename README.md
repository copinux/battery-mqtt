# Battery MQTT

Application Android d'exemple (Kotlin) qui publie le niveau de charge de la batterie sur un broker MQTT **toutes les 60 secondes**.

## Fonctionnement

- Interface simple : adresse du broker, port, topic, utilisateur/mot de passe (facultatifs).
- Un **service au premier plan** (notification persistante) garde la publication active même quand l'app est en arrière-plan.
- Le niveau de batterie est lu via `BatteryManager.BATTERY_PROPERTY_CAPACITY` (avec repli sur l'intent `ACTION_BATTERY_CHANGED`).
- Publication MQTT QoS 1 avec reconnexion automatique (bibliothèque Eclipse Paho).
- La valeur publiée est un simple entier : `0` à `100`.
- Configuration sauvegardée dans les préférences de l'app.

## Build

1. Ouvre le projet dans **Android Studio** (ou utilise le wrapper Gradle).
2. Branche un téléphone en mode débogage USB (ou lance un émulateur).
3. `./gradlew assembleDebug` puis installe `app/build/outputs/apk/debug/app-debug.apk`.

## Utilisation

1. Saisis l'adresse du broker (ex. `test.mosquitto.org` pour un test public, ou l'IP de ton broker local type Mosquitto).
2. Port : `1883` (MQTT non chiffré) par défaut.
3. Topic : `battery/level` par défaut.
4. Appuie sur **Démarrer l'envoi**. Une notification affiche la dernière valeur publiée.

Pour vérifier côté broker :

```bash
mosquitto_sub -h <broker> -t "battery/level" -v
```

## Notes

- Android 13+ demande la permission de notification au premier affichage.
- Sur Android 14+, le service utilise le type `dataSync` ; pour une utilisation prolongée en production, envisage le type `specialUse` ou `connectedDevice`.
- Le broker public `test.mosquitto.org` ne doit servir qu'à des tests : tout message y est public.
