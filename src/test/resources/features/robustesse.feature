# language: fr
Fonctionnalité: Robustesse - statuts forcés et requêtes invalides
  En tant qu'équipe QA,
  je veux forcer le comportement du prestataire par configuration
  afin de vérifier la réaction de l'application testée aux pannes du PSP.

  Scénario: Le mode forcé ERROR renvoie une erreur technique
    Étant donné le mock démarré avec le statut forcé "ERROR" et le code d'erreur "PSP_UNAVAILABLE"
    Et une demande de paiement de 49,90 € par carte "VISA" pour le client "C-400"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 500
    Et le statut du paiement est "ERROR"
    Et le code d'erreur est "PSP_UNAVAILABLE"

  Scénario: Le mode forcé DECLINED refuse tous les paiements
    Étant donné le mock démarré avec le statut forcé "DECLINED"
    Et une demande de paiement de 49,90 € par carte "VISA" pour le client "C-401"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 200
    Et le statut du paiement est "DECLINED"

  Scénario: Le mode forcé TIMEOUT simule une panne du prestataire
    Étant donné le mock démarré avec le statut forcé "TIMEOUT"
    Et une demande de paiement de 49,90 € par carte "VISA" pour le client "C-402"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 504
    Et le statut du paiement est "TIMEOUT"
    Et le code d'erreur est "PSP_TIMEOUT"

  Scénario: Une requête sans montant est rejetée
    Quand j'envoie la requête brute '{"cardType": "VISA", "customerId": "C-403"}'
    Alors le code HTTP est 400
