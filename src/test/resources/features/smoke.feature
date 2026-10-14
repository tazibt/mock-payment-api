# language: fr
@smoke
Fonctionnalité: Smoke tests du mock de paiement
  Vérifications rapides exécutées après chaque déploiement :
  si l'une d'elles échoue, la campagne de test est bloquée.

  Scénario: Le service répond
    Quand je consulte l'état du service
    Alors le code HTTP est 200
    Et le corps de la réponse est "UP"

  Scénario: Le parcours nominal de paiement fonctionne
    Étant donné une demande de paiement de 49,90 € par carte "VISA" pour le client "C-001"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 200
    Et le statut du paiement est "ACCEPTED"
