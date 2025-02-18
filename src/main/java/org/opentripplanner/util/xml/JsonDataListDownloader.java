package org.opentripplanner.util.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import lombok.Setter;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class JsonDataListDownloader<T> {

  private static final Logger log = LoggerFactory.getLogger(JsonDataListDownloader.class);

  @Setter
  private String url;
  private final String jsonParsePath;
  private final String headerName;
  private final String headerValue;
  private final Function<JsonNode, T> elementParser;

  public JsonDataListDownloader(
      String url, String jsonParsePath, Function<JsonNode, T> elementParser, String headerName, String headerValue
  ) {
    this.url = url;
    this.jsonParsePath = jsonParsePath;
    this.headerName = headerName;
    this.headerValue = headerValue;
    this.elementParser = elementParser;
  }

  public List<T> download() {
    if (url == null) {
      log.warn("Cannot download updates, because url is null!");
      return null;
    }

    try (InputStream data = openInputStream()) {
      if (data == null) {
        log.warn("Failed to get data from url " + url);
        return null;
      }
      return parseJSON(data);
    } catch (IllegalArgumentException e) {
      log.warn("Error parsing bike rental feed from " + url, e);
    } catch (JsonProcessingException e) {
      log.warn("Error parsing bike rental feed from " + url + "(bad JSON of some sort)", e);
    } catch (IOException e) {
      log.warn("Error reading bike rental feed from " + url, e);
    }
    return null;
  }

  private InputStream openInputStream() throws IOException {
    URL downloadUrl = new URL(url);
    String proto = downloadUrl.getProtocol();
    if (proto.equals("http") || proto.equals("https")) {
      return HttpUtils.getData(URI.create(url), Collections.emptyMap());
    } else {
      // Local file probably, try standard java
      return downloadUrl.openStream();
    }
  }

  private List<T> parseJSON(InputStream dataStream) throws IllegalArgumentException, IOException {

    ArrayList<T> out = new ArrayList<>();

    String rentalString = convertStreamToString(dataStream);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readTree(rentalString);

    if (!jsonParsePath.equals("")) {
      String delimiter = "/";
      String[] parseElement = jsonParsePath.split(delimiter);
      for (String s : parseElement) {
        rootNode = rootNode.path(s);
      }

      if (rootNode.isMissingNode()) {
        throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
      }
    }

    for (JsonNode node : rootNode) {
      if (node == null) {
        continue;
      }
      T parsedElement = elementParser.apply(node);
      if (parsedElement != null) {
        out.add(parsedElement);
      }
    }
    return out;
  }

  private static String convertStreamToString(java.io.InputStream is) {
    try (java.util.Scanner scanner = new java.util.Scanner(is).useDelimiter("\\A")) {
      return scanner.hasNext() ? scanner.next() : "";
    }
  }
}
