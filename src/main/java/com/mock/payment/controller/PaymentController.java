package com.mock.payment.controller;

import com.mock.payment.model.PaymentRequest;
import com.mock.payment.model.PaymentResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    private static final BigDecimal TIMEOUT_AMOUNT = new BigDecimal("999.99");

    @Value("${mock.delay.success}")
    private int delaySuccess;

    @Value("${mock.delay.timeout}")
    private int delayTimeout;

    @Value("${mock.delay.error}")
    private int delayError;

    @Value("${mock.force.status}")
    private String forcedStatus;

    @Value("${mock.force.errorCode}")
    private String forcedErrorCode;

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    // Utilisé par le HEALTHCHECK Docker et les smoke tests
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("UP");
    }

    @PostMapping("/mock")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest req) throws InterruptedException {

        log.info("Paiement reçu : montant={}, carte={}, client={}",
                req.amount, req.cardType, req.customerId);

        // 1. Statut forcé (piloté par config)
        if (!forcedStatus.equalsIgnoreCase("NONE")) {
            return simulateForcedStatus();
        }

        // 2. Timeout simulé
        if (BigDecimal.valueOf(req.amount).compareTo(TIMEOUT_AMOUNT) == 0) {
            return timeout();
        }

        // 3. Carte expirée
        if ("EXPIRED".equalsIgnoreCase(req.cardType)) {
            Thread.sleep(delayError);
            return ResponseEntity.ok(declined("Carte expirée", "CARD_EXPIRED"));
        }

        // 4. Paiement accepté
        Thread.sleep(delaySuccess);
        return ResponseEntity.ok(accepted());
    }

    private ResponseEntity<PaymentResponse> simulateForcedStatus() throws InterruptedException {
        switch (forcedStatus.toUpperCase()) {
            case "TIMEOUT":
                return timeout();

            case "DECLINED":
                Thread.sleep(delayError);
                return ResponseEntity.ok(declined("Paiement refusé", forcedErrorCode));

            case "ERROR":
                Thread.sleep(delayError);
                PaymentResponse error = new PaymentResponse("ERROR", "Erreur technique");
                error.errorCode = forcedErrorCode;
                return ResponseEntity.status(500).body(error);

            default:
                log.warn("mock.force.status inconnu : '{}' -> paiement accepté par défaut", forcedStatus);
                Thread.sleep(delaySuccess);
                return ResponseEntity.ok(accepted());
        }
    }

    private ResponseEntity<PaymentResponse> timeout() throws InterruptedException {
        Thread.sleep(delayTimeout);
        PaymentResponse resp = new PaymentResponse("TIMEOUT", "Le prestataire n'a pas répondu à temps");
        resp.errorCode = "PSP_TIMEOUT";
        return ResponseEntity.status(504).body(resp);
    }

    private static PaymentResponse declined(String message, String errorCode) {
        PaymentResponse resp = new PaymentResponse("DECLINED", message);
        resp.errorCode = errorCode;
        return resp;
    }

    // UUID : identifiant unique même sous forte concurrence (tests de charge)
    private static PaymentResponse accepted() {
        PaymentResponse resp = new PaymentResponse("ACCEPTED", "Paiement autorisé");
        resp.transactionId = "TX-" + UUID.randomUUID();
        return resp;
    }
}
