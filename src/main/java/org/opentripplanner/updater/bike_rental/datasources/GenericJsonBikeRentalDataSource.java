package org.opentripplanner.updater.bike_rental.datasources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.GeofencingZones;
import org.opentripplanner.routing.bike_rental.GeofencingZones.GeofencingZone;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Fetch Bike Rental JSON feeds and pass each record on to the specific rental subclass
 *
 * @see BikeRentalDataSource
 */
abstract class GenericJsonBikeRentalDataSource<T extends BikeRentalDataSourceParameters> implements BikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(GenericJsonBikeRentalDataSource.class);
    protected final T config;
    private String url;
    private final Map<String, String> headers;

    private final String jsonParsePath;

    List<BikeRentalStation> stations = new ArrayList<>();
    Set<GeofencingZone> geofencingZones = new HashSet<>();

    /**
     * Construct superclass
     *
     * @param jsonPath JSON path to get from enclosing elements to nested rental list.
     *        Separate path levels with '/' For example "d/list"
     *
     */
    public GenericJsonBikeRentalDataSource(
        T config,
        String jsonPath
    ) {
        this.config = config;
        url = config.getUrl();
        jsonParsePath = jsonPath;
        headers = config.getHttpHeaders();
    }

    /**
     *
     * @param jsonPath path to get from enclosing elements to nested rental list.
     *        Separate path levels with '/' For example "d/list"
     * @param headers http headers
     */
    public GenericJsonBikeRentalDataSource(
        T config,
        String jsonPath,
        Map<String, String> headers
    ) {
        this.config = config;
        this.url = config.getUrl();
        this.jsonParsePath = jsonPath;
        this.headers = headers;
    }

    @Override
    public boolean update() {
        if (url == null) { return false; }

        try {
            InputStream data;
        	
        	URL url2 = new URL(url);
        	
            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
            	data = HttpUtils.getData(URI.create(url), headers);
            } else {
                // Local file probably, try standard java
                data = url2.openStream();
            }
            // TODO handle optional GBFS files, where it's not warning-worthy that they don't exist.
            if (data == null) {
                log.warn("Failed to get data from url " + url);
                return false;
            }
            parseJSON(data);
            data.close();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing bike rental feed from " + url, e);
            return false;
        } catch (JsonProcessingException e) {
            log.warn("Error parsing bike rental feed from " + url + "(bad JSON of some sort)", e);
            return false;
        } catch (IOException e) {
            log.warn("Error reading bike rental feed from " + url, e);
            return false;
        }
        return true;
    }

    private void parseJSON(InputStream dataStream) throws IllegalArgumentException, IOException {

        List<BikeRentalStation> stations = new ArrayList<>();
        Set<GeofencingZone> geofencingZones = new HashSet<>();

        String rentalString = convertStreamToString(dataStream);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(rentalString);

        if (!jsonParsePath.equals("")) {
            String delimiter = "/";
            String[] parseElement = jsonParsePath.split(delimiter);
            for(int i =0; i < parseElement.length ; i++) {
                rootNode = rootNode.path(parseElement[i]);
            }

            if (rootNode.isMissingNode()) {
                throw new IllegalArgumentException("Could not find jSON elements " + jsonParsePath);
              }
        }

        for (int i = 0; i < rootNode.size(); i++) {
            // TODO can we use foreach? for (JsonNode node : rootNode) ...
            JsonNode node = rootNode.get(i);
            if (node == null) {
                continue;
            }
            makeStation(node).ifPresent(stations::add);
            makeGeofencingZone(node).ifPresent(geofencingZones::add);
        }

        synchronized(this) {
            this.stations = stations;
            this.geofencingZones = geofencingZones;
        }
    }

    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner scanner = null;
        String result;
        try {
           
            scanner = new java.util.Scanner(is).useDelimiter("\\A");
            result = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        finally
        {
           if(scanner!=null) {
               scanner.close();
           }
        }
        return result;
        
    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

    @Override
    public synchronized GeofencingZones getGeofencingZones() {
        return new GeofencingZones(geofencingZones);
    }

    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
    	this.url = url;
    }

    public abstract Optional<BikeRentalStation> makeStation(JsonNode rentalStationNode);
    public abstract Optional<GeofencingZone> makeGeofencingZone(JsonNode geofencingZoneNode);

    @Override
    public String toString() {
        return getClass().getName() + "(" + url + ")";
    }
}
