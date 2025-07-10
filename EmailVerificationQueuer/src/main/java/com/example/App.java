package com.example;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import brevo.ApiClient;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.TransactionalEmailsApi;
import brevoModel.SendSmtpEmail;
import brevoModel.SendSmtpEmailSender;
import brevoModel.SendSmtpEmailTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;



public class App implements RequestHandler<LinkedHashMap<String, Object>, Object> {

    public SendSmtpEmail smtpEmailSetup(JsonNode body) {
        String htmlContent = "<html><head></head><body><p>Hello,</p>This is my first transactional email sent from Brevo.</p></body></html>";
        String userEmail = body.get("Email").textValue();

        SendSmtpEmail smtpEmail = new SendSmtpEmail();
        smtpEmail.setSubject("Email Verification");
        smtpEmail.sender(smtpEmailSenderSetup());
        smtpEmail.to(recepientListSetup(userEmail)); 
        smtpEmail.htmlContent(htmlContent);

        return smtpEmail;
    }

    public SendSmtpEmailSender smtpEmailSenderSetup() {
        // Set sender info
        SendSmtpEmailSender sender = new SendSmtpEmailSender();
        sender.setEmail(System.getenv("SENDER_EMAIL"));
        sender.setName("CloudTicket");

        return sender;
    }

    public List<SendSmtpEmailTo> recepientListSetup(String userEmail) {
        // Set receiver info
        SendSmtpEmailTo recepient = new SendSmtpEmailTo();
        recepient.setEmail(userEmail);

        return List.of(recepient);
    }

    public String sendBrevoTransacEmail(JsonNode body) {
         // Build apiClient for Brevo API
         ApiClient defaultClient = Configuration.getDefaultApiClient();

         // REQUIRED FOR  BREVO API CALLS
         ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
         apiKey.setApiKey(System.getenv("CloudTicket_Brevo_API_Key"));
 
         // Set up transaction email API and SendSMTPEmail
         TransactionalEmailsApi transacEmailInstance = new TransactionalEmailsApi();
         SendSmtpEmail smtpEmail = smtpEmailSetup(body);
         try {
            return transacEmailInstance.sendTransacEmail(smtpEmail).toString();
         } catch (Exception e) {
            return "Error: " + e + ":(";
         } 
    }
    
    public void sendSQSMessage(String jsonStringMessage) {
        // Create an SQS Client to execute SQS services
        // Note: An AWS Crt HTTP client is used as the SQS dependency 
        // has an exclusion of an apache client and netty nio client
        // within the AWS SQS Documentation for sending messages
        SqsClient sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .httpClient(AwsCrtHttpClient.create())
            .build();

        // Build a SendMessageRequest object to fill in the sendMessage() parameter
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
            .queueUrl(System.getenv("AWS_VERIFIEDEMAILQUEUE_SQS_URL"))
            .messageBody(jsonStringMessage)
            .messageGroupId("verification")
            .messageDeduplicationId(UUID.randomUUID().toString())
            .delaySeconds(0)
            .build();      
                
        sqsClient.sendMessage(sendMessageRequest);
        // Note: the user will not be able to click the Sign-Up button until
        // the 2 minute ewindow expires or the account verification is successful
        // as to prevent potential UUID collisions
    }

    public String postRequestHandler() {
        return null;
    }

    @Override
    public Object handleRequest(LinkedHashMap<String, Object> input, Context context) {
        // Convert given LinkedHashMap<String, Object> input into JsonNode
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.valueToTree(input);

        return switch (json.get("routeKey").textValue()) {
            case "POST /ligmaballs" -> postRequestHandler();
            default -> "L + ratio + rip bozo XD";
        };
        /*
        // execute email sending if POST request to correct api endpoint
        if ("POST /ligmaballs".equals(json.get("routeKey").textValue())) {
        String bodyString = json.get("body").textValue();
        try {
        JsonNode body = objectMapper.readTree(bodyString);
        try {
        sendSQSMessage(bodyString);
        return sendBrevoTransacEmail(body);
        } catch (Exception e) {
        return e.getMessage();
        }
        } catch (Exception e) {
        return "Error occurred parsing JSON :(";
        }
        } else {
        return "Goofy routekey/method detected :P";
        }
         */
    }
}
