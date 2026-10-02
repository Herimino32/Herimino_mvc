# Mon Framework MVC (Inspiré de Spring MVC)

## Sprint 0 : Structure de base & Front Controller
Mise en place de la servlet centralisée `FrontControllerServlet`. Le framework intercepte toutes les requêtes entrantes pour analyser l'URL demandée par le client.

## Sprint 1 : Scan et cartographie des routes
Détection dynamique des classes annotées avec `@Controller` et de leurs méthodes annotées avec `@MethodAnnotation`. Les routes sont enregistrées dans un dictionnaire de mapping.

## Sprint 2 : Invocation dynamique des méthodes
Utilisation de la réflexion Java (`java.lang.reflect`) pour instancier le contrôleur et exécuter la méthode correspondant à l'URL demandée.

## Sprint 3 : Gestion des vues avec ModelView
Introduction de la classe `ModelView`. Le contrôleur retourne le nom de la vue et les données associées. Le framework injecte ces données dans les attributs de la requête et effectue une redirection (`forward`) vers `/WEB-INF/views/`.

## Sprint 4 : Support des ressources statiques
Prise en charge des fichiers statiques (`.css`, `.js`, `.png`, `.jpg`, etc.) afin qu'ils soient servis directement par le serveur sans passer par le contrôleur.

## Sprint 5 : Gestion des verbes HTTP (GET & POST)
Distinction des requêtes en fonction de la méthode HTTP employée. La classe `UrlMethod` permet de différencier une route `GET` d'une route `POST` sur une même URL.

## Sprint 6 : Support des API REST (@Restapi & JSON)
Ajout de l'annotation `@Restapi`. Lorsqu'une méthode est annotée, le framework convertit le résultat en JSON via `JsonUtils` et le renvoie directement dans la réponse HTTP.

## Sprint 7 : Injection dynamique des paramètres (String)
Lecture automatique des paramètres de la requête HTTP (`request.getParameter()`) et injection dynamique dans les arguments de la méthode du contrôleur (nécessite la compilation avec l'option `-parameters`).
## sprint 7 bis :
- argument objet instance objet 