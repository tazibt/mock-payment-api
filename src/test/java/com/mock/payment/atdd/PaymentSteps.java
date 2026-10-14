package com.mock.payment.atdd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.mock.payment.MockPaymentApplication;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class PaymentSteps {

    private static ConfigurableApplicationContext app;
    private static int defaultPort;

    // lancé seulement pour les scénarios en mode forcé
    private ConfigurableApplicationContext forcedApp;
    private Map<String, Object> request;
    private Response response;

    @BeforeAll
    public static void startMock() {
        app = start();
        defaultPort = port(app);
    }

    @AfterAll
    public static void stopMock() {
        app.close();
    }

    @After
    public void stopForcedMock() {
        if (forcedApp != null) {
            forcedApp.close();
        }
        RestAssured.port = defaultPort;
    }

    private static ConfigurableApplicationContext start(String... extraArgs) {
        List<String> args = new ArrayList<>(List.of(
                "--server.port=0",
                // délais réduits sinon les tests sont trop longs
                "--mock.delay.timeout=300",
                "--mock.delay.error=100"));
        args.addAll(List.of(extraArgs));
        return SpringApplication.run(MockPaymentApplication.class, args.toArray(String[]::new));
    }

    private static int port(ConfigurableApplicationContext context) {
        int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
        RestAssured.port = port;
        return port;
    }

    @Given("le mock démarré avec le statut forcé {string}")
    public void mockForce(String status) {
        forcedApp = start("--mock.force.status=" + status);
        port(forcedApp);
    }

    @Given("le mock démarré avec le statut forcé {string} et le code d'erreur {string}")
    public void mockForceAvecCode(String status, String errorCode) {
        forcedApp = start("--mock.force.status=" + status, "--mock.force.errorCode=" + errorCode);
        port(forcedApp);
    }

    @When("j'envoie la requête brute {string}")
    public void envoyerRequeteBrute(String body) {
        response = given().contentType(ContentType.JSON).body(body).post("/payment/mock");
    }

    @Given("une demande de paiement de {double} € par carte {string} pour le client {string}")
    public void unePaymentRequest(double amount, String cardType, String customerId) {
        request = Map.of("amount", amount, "cardType", cardType, "customerId", customerId);
    }

    @When("j'envoie la demande de paiement")
    public void envoyerPaiement() {
        response = given().contentType(ContentType.JSON).body(request).post("/payment/mock");
    }

    @When("je consulte l'état du service")
    public void consulterEtat() {
        response = given().get("/payment/health");
    }

    @Then("le code HTTP est {int}")
    public void codeHttp(int status) {
        response.then().statusCode(status);
    }

    @Then("le corps de la réponse est {string}")
    public void corpsReponse(String body) {
        response.then().body(equalTo(body));
    }

    @Then("le statut du paiement est {string}")
    public void statutPaiement(String status) {
        response.then().body("status", equalTo(status));
    }

    @Then("le code d'erreur est {string}")
    public void codeErreur(String errorCode) {
        response.then().body("errorCode", equalTo(errorCode));
    }

    @Then("un identifiant de transaction est retourné")
    public void identifiantTransaction() {
        response.then().body("transactionId", startsWith("TX-"));
    }
}
