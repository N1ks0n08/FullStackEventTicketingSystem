package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;
import software.amazon.awssdk.services.sqs.model.*;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

import software.amazon.awssdk.services.sqs.SqsClient;

public class App implements RequestHandler<Object, Object> {
    // get connection to Aurora DSQL
    public static Connection getConnection(String clusterEndpoint, String region) throws SQLException {
        Properties props = new Properties();
         // Use the DefaultJavaSSLFactory so that Java's default trust store can be used
        // to verify the server's root cert.
        String url = "jdbc:postgresql://" + clusterEndpoint + ":5432/postgres?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory";

        // create a dsqutil instance to generate a Aurora DSQL authentication token
        DsqlUtilities dsqlutil = DsqlUtilities.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().asyncCredentialUpdateEnabled(true).build())
            .build();  
        
        String password = dsqlutil.generateDbConnectAdminAuthToken(
            GenerateAuthTokenRequest.builder().hostname(clusterEndpoint).build());

        props.setProperty("user", "admin");
        props.setProperty("password", password);
        return DriverManager.getConnection(url, props);
    }
    
    public static String executePSQLCommands(JsonNode input) {
        // Replace the cluster endpoint with your own
        String clusterEndpoint = System.getenv("AWS_AURORA_DSQL_CLUSTER_ENDPOINT");
        String region = "us-east-1";
        try (Connection conn = App.getConnection(clusterEndpoint, region)) {
           String name = input.get("Name").textValue();
           String city = input.get("City").textValue();
           String email = input.get("Email").textValue();
           // Insert some data
           UUID uuid = UUID.randomUUID();
           PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (id, name, city, email) VALUES (?, ?, ?, ?)");
           pstmt.setObject(1, uuid);
           pstmt.setString(2, name);
           pstmt.setString(3, city);
           pstmt.setString(4, email);
           pstmt.executeUpdate();
           conn.close();
           pstmt.close();

           return "Successfuly added to Aurora DSQL database! :D";
        } catch (SQLException sqlE) {
            return "Error code: " + sqlE.getErrorCode();
        }
    }

    public Message getQueueMessage() {
        SqsClient sqsClient = SqsClient.create();
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
            .queueUrl(System.getenv("AWS_VERIFIEDEMAILQUEUE_SQS_URL"))
            .maxNumberOfMessages(1)
            .build();

        return sqsClient.receiveMessage(receiveRequest).messages().get(0);
    }

    public void removeQueueMessage() {
        // remove messages after processing
    }

    /*

    public static void executPSQLCommandsTest() {
        // Replace the cluster endpoint with your own
        String clusterEndpoint = System.getenv(YOUR_AURORA_DSQL_CLUSTER_ENDPOINT);
        String region = "us-east-1";
        Scanner scan = new Scanner(System.in);
        try (Connection conn = App.getConnection(clusterEndpoint, re gion)) {
           // Ask for name, city, and phone number
           System.out.println("Please enter your name: ");
           String name = scan.nextLine();
           System.out.println("Please enter your city: ");
           String city = scan.nextLine();
           System.out.println("Please enter your phone number: ");
           String phoneNumber = scan.nextLine();
           scan.close();
           // Insert some data
           UUID uuid = UUID.randomUUID();
           PreparedStatement pstmt = conn.prepareStatement("INSERT INTO owner (id, name, city, telephone) VALUES (?, ?, ?, ?)");
           pstmt.setObject(1, uuid);
           pstmt.setString(2, name);
           pstmt.setString(3, city);
           pstmt.setString(4, phoneNumber);

           pstmt.executeUpdate();
           conn.close();
           pstmt.close();
        } catch (SQLException sqlE) {
            System.out.println("Error: " + sqlE.getMessage());
        }
    }

    */
    
    @Override
    public Object handleRequest(Object input, Context context) {
        ObjectMapper objectMapper = new ObjectMapper();
        Message queueMessage = getQueueMessage();
        JsonNode json;
        try {
            json = objectMapper.readTree(queueMessage.body());
            return json.toPrettyString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }
    
    /*
    public static void main(String[] args) {
        executPSQLCommandsTest();
    }
    */
    
}