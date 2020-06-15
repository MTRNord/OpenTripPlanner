/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for German roads

 * Speed limits where adjusted to German regulation and some bike safety settings tweaked,
 * especially including tracktype's grade and preference of bicycle networks.
 *
 * @author hbruch
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */
public class GermanyWayPropertySetSource implements WayPropertySetSource {

	@Override
	public void populateProperties(WayPropertySet props) {
        // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements

        /*
         * Automobile speeds in Germany. General speed limit is 50kph in settlements, 100kph outside settlements.
         * For motorways, there (currently still) is no limit. Nevertheless 120kph is assumed to reflect varying
         * traffic conditions
         *
         */
        props.setCarSpeed("highway=motorway", 33.33f); // = 120kph. Varies between 80 - 120 kph depending on road and season.
        props.setCarSpeed("highway=motorway_link", 15); // = 54kph
        props.setCarSpeed("highway=trunk", 27.27f); // 100kph
        props.setCarSpeed("highway=trunk_link", 15); // = 54kph
        props.setCarSpeed("highway=primary", 27.27f); // 100kph
        props.setCarSpeed("highway=primary_link", 15); // = 54kph

        // you should only use parking aisle if there is no other options
        // ideally they would be set to noThruTraffic but that would mean the parking lots are inaccessible
        props.setCarSpeed("service=parking_aisle", 5);

        /*
         * Many agricultural ways are tagged as 'track' but have no access tags. We assume this to mean that cars
         * are prohibited.
         * https://www.openstreetmap.org/way/124263424
         */
        props.setProperties("highway=track", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);
        props.setProperties("highway=track;surface=*", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.0, 1.0);

        props.setProperties("highway=residential;junction=roundabout", StreetTraversalPermission.ALL, 0.98, 0.98);
        props.setProperties("highway=*;junction=roundabout", StreetTraversalPermission.BICYCLE_AND_CAR);

        // Pedestrian zones in Germany are forbidden for bicycles by default
        props.setProperties("highway=pedestrian", StreetTraversalPermission.PEDESTRIAN);
        props.setProperties("highway=residential;maxspeed=30", StreetTraversalPermission.ALL, 0.9, 0.9);
        props.setProperties("highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 0.8, 0.8);
        // Default was 2.5, we want to favor using mixed footways somewhat
        props.setProperties("footway=sidewalk;highway=footway;bicycle=yes",
                StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 1.2, 1.2);

        /** tracktype */
        props.setProperties("tracktype=grade1", StreetTraversalPermission.ALL, 1.0, 1.0, true); // Solid
        props.setProperties("tracktype=grade2", StreetTraversalPermission.ALL, 1.1, 1.1, true); // Solid but unpaved.
        props.setProperties("tracktype=grade3", StreetTraversalPermission.ALL, 1.15, 1.15, true); // Mostly solid.
        props.setProperties("tracktype=grade4", StreetTraversalPermission.ALL, 1.3, 1.3, true); // Mostly soft.
        props.setProperties("tracktype=grade5", StreetTraversalPermission.ALL, 1.5, 1.5, true); // Soft.

        /** We assume highway/cycleway of a cycle network to be safer (for bicycle network relations, their network is copied to way in postLoad \
         *
         * This uses a newly added logical OR since you don't want to apply the safety multiplier more than once.
         * */
        props.setProperties("lcn=yes|rcn=yes|ncn=yes", StreetTraversalPermission.ALL, 0.7, 0.7, true);

        // lit=yes currently is tagged very seldom, so we just want to discount where lit=no explicitly
        // props.setProperties("lit=yes", StreetTraversalPermission.ALL, 0.99, 0.99, true); // lit increases safety
        props.setProperties("lit=no", StreetTraversalPermission.ALL, 1.05, 1.05, true); // not lit decreases safety


       /**
        * Artifical bicycle bonus/malus for when you want to achieve a certain route, for example when planning
        * diversions
        */
        props.setProperties("bicycle:bonus=yes", StreetTraversalPermission.ALL, 0.2, 0.2, true);
        props.setProperties("bicycle:malus=yes", StreetTraversalPermission.ALL, 2.5, 2.5, true);

        // Read the rest from the default set
		new DefaultWayPropertySetSource().populateProperties(props);
	}
}
