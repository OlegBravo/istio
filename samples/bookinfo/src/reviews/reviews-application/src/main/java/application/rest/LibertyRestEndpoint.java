/*******************************************************************************
 * Copyright (c) 2017 Istio Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package main.java.application.rest;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Random;

/*
 no stars -  disk
 black star (1)- cpu
 red start (2) - network
 */

@Path("/")
public class LibertyRestEndpoint extends Application {

    private final static Boolean ratings_enabled = Boolean.valueOf(System.getenv("ENABLE_RATINGS"));
    private final static String star_color = System.getenv("STAR_COLOR") == null ? "black" : System.getenv("STAR_COLOR");
    private final static String services_domain = System.getenv("SERVICES_DOMAIN") == null ? "" : ("." + System.getenv("SERVICES_DOMAIN"));
    private final static String ratings_hostname = System.getenv("RATINGS_HOSTNAME") == null ? "ratings" : System.getenv("RATINGS_HOSTNAME");
    private final static String ratings_service = "http://" + ratings_hostname + services_domain + ":9080/ratings";
    // HTTP headers to propagate for distributed tracing are documented at
    // https://istio.io/docs/tasks/telemetry/distributed-tracing/overview/#trace-context-propagation
    private final static String[] headers_to_proagate = {"x-request-id","x-b3-traceid","x-b3-spanid","x-b3-sampled","x-b3-flags",
            "x-ot-span-context","x-datadog-trace-id","x-datadog-parent-id","x-datadog-sampled", "end-user","user-agent"};

    class LoadThread implements Runnable {

        boolean ratings_enabled;
        int starsReviewer1;
        int starsReviewer2;

        private LoadThread(boolean ratings_enabled, int starsReviewer1, int starsReviewer2) {
            this.ratings_enabled = ratings_enabled;
            this.starsReviewer1 = starsReviewer1;
            this.starsReviewer2 = starsReviewer2;
        }

        public void run() {
            try {
                if (!ratings_enabled) {
                    LoadNetwork loadNetwork = new LoadNetwork();
                    if  (Thread.currentThread().isInterrupted()) {
                        loadNetwork.interrupt();
                    }
                } else if (starsReviewer1 != -1) {
                    LoadThreadCPU loadThreadCPU = new LoadThreadCPU();
                    loadThreadCPU.run();
                    if ( Thread.currentThread().isInterrupted() ) {
                        loadThreadCPU.interrupt();
                    }
                } else if (starsReviewer2 != -1) {
                    LoadThreadDiskIO loadThreadDiskIO  = new LoadThreadDiskIO();
                    loadThreadDiskIO.run();
                    if( Thread.currentThread().isInterrupted() ) {
                        loadThreadDiskIO.interrupt();
                    }
                } else {
                    System.err.println("Unpredicted request type");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        public  void  interrupt() {
            Thread.currentThread().interrupt();
        }

        private class LoadThreadCPU implements Runnable {
            public void run() {
                double val = 10;
                while (true) {
                    val = Math.atan(Math.sqrt(Math.pow(val, 10)));
                }
            }
            public void interrupt() {
                Thread.currentThread().interrupt();
            }
        }

        private class LoadThreadDiskIO implements Runnable {
            java.nio.file.Path fullPath;
            public void run() {
                fullPath = new File("/var/log/", "tmp.diskload").toPath();
                try {
                    interrupt();
                    try {
                        Files.createDirectories(fullPath.getParent());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    Random rnd = new Random();
                    try (BufferedWriter bw = Files.newBufferedWriter(fullPath)) {
                        while (!Thread.currentThread().isInterrupted()) {
                            String line = String.format("%s %s%n", rnd.nextDouble(), rnd.nextDouble());
                            bw.write(line);
                        }
                    }
                }
                catch (IOException e) {
                    System.out.println("Can`t write to file");
                    System.exit(2);
                }
            }
            public  void  interrupt() {
                try {
                    Files.deleteIfExists(fullPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                Thread.currentThread().interrupt();
            }
        }

        private class LoadNetwork implements Runnable {
            String url = "https://testtf-bravo.s3.amazonaws.com/contrail-install-packages_5.1-797-rhel-queens.tar";
            public void run() {
                try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
                     FileOutputStream fileOutputStream = new FileOutputStream("/dev/null/")) {
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    System.err.println("Can`t download file :" +
                            e.getMessage());
                    // handle exception
                }

            }

            public void interrupt() {
                Thread.currentThread().interrupt();
            }

        }
    }


    private String getJsonResponse(String productId, int starsReviewer1, int starsReviewer2) {
        LoadThread loadThread = new LoadThread( ratings_enabled,  starsReviewer1,  starsReviewer2);
        loadThread.run();

        String result = "{";
        result += "\"id\": \"" + productId + "\",";
        result += "\"reviews\": [";

        // reviewer 1:
        result += "{";
        result += "  \"reviewer\": \"Reviewer1\",";
        result += "  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"";
        if (ratings_enabled) {
            if (starsReviewer1 != -1) {
                result += ", \"rating\": {\"stars\": " + starsReviewer1 + ", \"color\": \"" + star_color + "\"}";
            }
            else {
                result += ", \"rating\": {\"error\": \"Ratings service is currently unavailable\"}";
            }
        }
        result += "},";

        // reviewer 2:
        result += "{";
        result += "  \"reviewer\": \"Reviewer2\",";
        result += "  \"text\": \"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"";
        if (ratings_enabled) {
            if (starsReviewer2 != -1) {
                result += ", \"rating\": {\"stars\": " + starsReviewer2 + ", \"color\": \"" + star_color + "\"}";
            }
            else {
                result += ", \"rating\": {\"error\": \"Ratings service is currently unavailable\"}";
            }
        }
        result += "}";

        result += "]";
        result += "}";
        loadThread.interrupt();
        return result;
    }

    private JsonObject getRatings(String productId, HttpHeaders requestHeaders) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Integer timeout = star_color.equals("black") ? 10000 : 2500;
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", timeout);
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", timeout);
        Client client = cb.build();
        WebTarget ratingsTarget = client.target(ratings_service + "/" + productId);
        Invocation.Builder builder = ratingsTarget.request(MediaType.APPLICATION_JSON);
        for (String header : headers_to_proagate) {
            String value = requestHeaders.getHeaderString(header);
            if (value != null) {
                builder.header(header,value);
            }
        }
        try {
            Response r = builder.get();

            int statusCode = r.getStatusInfo().getStatusCode();
            if (statusCode == Response.Status.OK.getStatusCode()) {
                try (StringReader stringReader = new StringReader(r.readEntity(String.class));
                     JsonReader jsonReader = Json.createReader(stringReader)) {
                    return jsonReader.readObject();
                }
            } else {
                System.out.println("Error: unable to contact " + ratings_service + " got status of " + statusCode);
                return null;
            }
        } catch (ProcessingException e) {
            System.err.println("Error: unable to contact " + ratings_service + " got exception " + e);
            return null;
        }
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"status\": \"Reviews is healthy\"}").build();
    }

    @GET
    @Path("/reviews/{productId}")
    public Response bookReviewsById(@PathParam("productId") int productId, @Context HttpHeaders requestHeaders) {
        int starsReviewer1 = -1;
        int starsReviewer2 = -1;

        if (ratings_enabled) {
            JsonObject ratingsResponse = getRatings(Integer.toString(productId), requestHeaders);
            if (ratingsResponse != null) {
                if (ratingsResponse.containsKey("ratings")) {
                    JsonObject ratings = ratingsResponse.getJsonObject("ratings");
                    if (ratings.containsKey("Reviewer1")){
                        starsReviewer1 = ratings.getInt("Reviewer1");
                    }
                    if (ratings.containsKey("Reviewer2")){
                        starsReviewer2 = ratings.getInt("Reviewer2");
                    }
                }
            }
        }

        String jsonResStr = getJsonResponse(Integer.toString(productId), starsReviewer1, starsReviewer2);
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(jsonResStr).build();
    }
}
