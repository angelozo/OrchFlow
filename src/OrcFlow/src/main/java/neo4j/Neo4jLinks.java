package neo4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import models.LinkData;
import models.SwitchData;

import config.NEO4J_CONFIG;

public class Neo4jLinks {
    private static String passwd = NEO4J_CONFIG.DSN;
    public static String SERVER_ROOT_URI = NEO4J_CONFIG.KEY;

    public void links(ArrayList < SwitchData > arraySWD, LinkData linkD) throws URISyntaxException {
        URI nodeSRC = null, nodeDST = null;
        for (int i = 0; i < arraySWD.size(); i++) {
            if (arraySWD.get(i).getDPID().equals(linkD.getSrcS())) {
                nodeSRC = arraySWD.get(i).getLocation();
            } else if (arraySWD.get(i).getDPID().equals(linkD.getDstS())) {
                nodeDST = arraySWD.get(i).getLocation();
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("{ 'direction' : '" + linkD.getDirection() + "', 'dstport' : " + String.valueOf(linkD.getDstP()) +
            ", 'srcport' : " + String.valueOf(linkD.getSrcP()) + ", 'type' : '" + linkD.getType() +
            "', 'srcswitch' : '" + linkD.getSrcS() + "', 'dstswitch' : '" + linkD.getDstS() + "'}");

        addRelationship(nodeSRC, nodeDST, sb.toString(), linkD);
    }

    private static void addRelationship(URI startNode, URI endNode, String jsonAttributes, LinkData linkD)
    throws URISyntaxException {
        URI fromUri = new URI(SERVER_ROOT_URI + "index/relationship/index_1445034018615_1/?uniqueness=get_or_create");
        String relationshipJson = generateJsonRelationship(startNode, endNode, linkD, jsonAttributes);

        WebResource resource = Client.create().resource(fromUri);
        // POST JSON to the relationships URI
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
            .entity(relationshipJson).header("Authorization", passwd).post(ClientResponse.class);

        final String location = response.getLocation().toString();

        response.close();
    }

    private static String generateJsonRelationship(URI startNode, URI endNode, LinkData linkD,
        String...jsonAttributes) {
        StringBuilder sb = new StringBuilder();

        if (startNode instanceof URI && endNode instanceof URI) {
            sb.append("{ 'key' : 'SrcDst',");

            sb.append("'value' : '");
            sb.append(linkD.getSrcS() + linkD.getSrcP() + linkD.getDstS() + linkD.getDstP());

            sb.append("', ");

            sb.append(" 'start' : '");
            sb.append(startNode.toString());
            sb.append("', ");

            sb.append(" 'end' : '");
            sb.append(endNode.toString());
            sb.append("', ");

            sb.append("'type' : 'link'");
        }

        if (jsonAttributes == null || jsonAttributes.length < 1) {
            sb.append(" }");
        } else {
            sb.append(", ");
            sb.append("'properties' : ");
            for (int i = 0; i < jsonAttributes.length; i++) {
                sb.append(jsonAttributes[i]);
                if (i < jsonAttributes.length - 1) { // Miss off the final comma
                    sb.append(", ");
                }
            }
            sb.append(" }");
        }

        return sb.toString();
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static Object readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return jsonText;
        } finally {
            is.close();
        }
    }
}
