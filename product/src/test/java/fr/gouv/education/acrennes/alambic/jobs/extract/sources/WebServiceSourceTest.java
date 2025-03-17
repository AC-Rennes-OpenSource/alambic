package fr.gouv.education.acrennes.alambic.jobs.extract.sources;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.JobContext;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import junit.framework.TestCase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class WebServiceSourceTest extends TestCase {

    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testWebSourceSource() throws AlambicException, IOException, JDOMException, InterruptedException {
        final String myApiKey = "my_api_key";
        final String expectedRequestBody = "{\"message\": \"Hello, World!\"}";

        // Define a mock response
        final MockResponse mockResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(expectedRequestBody);

        // Enqueue the response
        mockWebServer.enqueue(mockResponse);

        // Get the base URL of the mock server
        final String baseUrl = mockWebServer.url("/").toString();

        final Properties configuration = new Properties();
        final Variables variables = new Variables();
        variables.put("ALAMBIC_TARGET_ENVIRONMENT", "TEST");
        variables.put("MY_API_URL", baseUrl);
        variables.put("MY_API_KEY", myApiKey);
        final CallableContext context = new JobContext(".", null, variables, configuration);

        // Create a SAXBuilder to parse the XML
        SAXBuilder saxBuilder = new SAXBuilder();
        final String xmlStringForWebSourceDefinition = "<resource type=\"webService\" name=\"API-resource\" dynamic=\"true\">\n" +
                "                    <uri>%MY_API_URL%</uri>\n" +
                "                    <method>GET</method>\n" +
                "                    <headers>\n" +
                "                        <header name=\"Authorization\">Bearer %MY_API_KEY%</header>\n" + // expression in header since 2.0.9
                "                        <header name=\"Content-Type\">application/json</header>\n" +
                "                    </headers>\n" +
                "                    <query/>\n" +
                "                    <response_codes>\n" +
                "                        <code type=\"success\">200</code>\n" +
                "                    </response_codes>\n" +
                "                </resource>";

        // Parse the XML string into a Document
        final Document document = saxBuilder.build(new StringReader(xmlStringForWebSourceDefinition));

        // Get the root element
        final Element sourceNode = document.getRootElement();

        final WebServiceSource source = new WebServiceSource(context, sourceNode);

        final String dynamicQuery = "{\"api\": \"V1/endpoint\",\"parameters\": \"param1=true&param2=false\" }";
        source.getClient().executeQuery(dynamicQuery);

        // Assert query path and headers
        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("/V1/endpoint?param1=true&param2=false", recordedRequest.getPath());
        assertEquals("Bearer " + myApiKey, recordedRequest.getHeader("Authorization"));
        assertEquals("application/json", recordedRequest.getHeader("Content-Type"));
        // Compare expected result to the one stored in StateBase
        assertEquals(expectedRequestBody, source.getClient().getStateBase().get(0).get("item").get(0));
    }
}