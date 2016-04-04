package com.github.tomakehurst.wiremock.osxit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OSXTest {

    @Test
    public void breakOSX() throws IOException, InterruptedException {
        WireMockConfiguration options = WireMockConfiguration.wireMockConfig()
                .dynamicPort().dynamicHttpsPort()
                .bindAddress("localhost");

        final WireMockServer server = new WireMockServer(options);

//        String proxyBaseUrl = "http://10.179.121.244:8084";
        String proxyBaseUrl = "http://localhost:8080";
//        final String path = "/";
        final String path = "/cd_webservice/odata.svc/Components(ItemId=273432,PublicationId=52)/ComponentPresentations";

        server.addStubMapping(
                new StubMapping(
                        RequestPattern.everything(),
                        ResponseDefinitionBuilder.responseDefinition().proxiedFrom(proxyBaseUrl).build()));

        server.start();

        ExecutorService pool = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 50; i++) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        URI uri = URI.create("http://localhost:" + server.port() + path);
                        HttpURLConnection urlConnection = (HttpURLConnection)uri.toURL().openConnection();
                        urlConnection.getHeaderFields().put("Accept", Collections.singletonList("application/json"));
                        urlConnection.connect();
                        IOUtils.copy(urlConnection.getInputStream(), new StringWriter());
                        urlConnection.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.MINUTES);
    }
}
