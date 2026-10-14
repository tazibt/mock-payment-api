# language: fr
Fonctionnalité: Autorisation d'un paiement
  En tant qu'équipe QA,
  je veux un mock de paiement au comportement prévisible
  afin de tester les parcours de commande sans dépendre d'un vrai prestataire.

  Règles métier (critères d'acceptation définis avant le développement) :
  - un paiement standard est accepté et reçoit un identifiant de transaction ;
  - une carte expirée est refusée avec le code CARD_EXPIRED ;
  - un montant de 999,99 € simule un timeout du prestataire (HTTP 504).

  Plan du scénario: Un paiement standard est accepté
    Étant donné une demande de paiement de <montant> € par carte "<carte>" pour le client "C-100"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 200
    Et le statut du paiement est "ACCEPTED"
    Et un identifiant de transaction est retourné

    Exemples:
      | montant | carte      |
      | 0,01    | VISA       |
      | 120,00  | MASTERCARD |
      | 5000,00 | AMEX       |

  Scénario: Une carte expirée est refusée
    Étant donné une demande de paiement de 80,00 € par carte "EXPIRED" pour le client "C-200"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 200
    Et le statut du paiement est "DECLINED"
    Et le code d'erreur est "CARD_EXPIRED"

  Scénario: Le prestataire ne répond pas à temps
    Étant donné une demande de paiement de 999,99 € par carte "VISA" pour le client "C-300"
    Quand j'envoie la demande de paiement
    Alors le code HTTP est 504
    Et le statut du paiement est "TIMEOUT"
    Et le code d'erreur est "PSP_TIMEOUT"
