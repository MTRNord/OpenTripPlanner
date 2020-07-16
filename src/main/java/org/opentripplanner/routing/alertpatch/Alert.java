package org.opentripplanner.routing.alertpatch;

import com.google.transit.realtime.GtfsRealtime;
import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.NonLocalizedString;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

public class Alert implements Serializable {
    private static final long serialVersionUID = 8305126586053909836L;

    public enum AlertId {
        CAR_PARK_FULL
    }

    public I18NString alertHeaderText;

    public I18NString alertDescriptionText;

    public I18NString alertUrl;

    public AlertId alertId;

    //null means unknown
    public Date effectiveStartDate;

    //null means unknown
    public Date effectiveEndDate;

    public GtfsRealtime.Alert.Effect effect;

    public GtfsRealtime.Alert.Cause cause;

    public GtfsRealtime.Alert.SeverityLevel severityLevel;

    public static HashSet<Alert> newSimpleAlertSet(String text) {
        Alert note = createSimpleAlerts(text);
        HashSet<Alert> notes = new HashSet<Alert>(1);
        notes.add(note);
        return notes;
    }

    public static Alert createSimpleAlerts(String text) {
        Alert note = new Alert();
        note.alertHeaderText = new NonLocalizedString(text);
        return note;
    }

    public static Alert createFloatingDropOffAlert() {
        return createTranslatedAlert("bicycle_rental.free_floating_dropoff");
    }

    public static Alert createLowCarParkSpacesAlert() {
        Alert alert = createTranslatedAlert("car_park.full");
        alert.alertId = AlertId.CAR_PARK_FULL;
        return alert;
    }

    private static Alert createTranslatedAlert(String translationKey) {
        var emptyArray = new String[]{};
        var alert = new Alert();
        alert.alertHeaderText= new LocalizedString("alert." + translationKey + ".header", emptyArray);
        alert.alertDescriptionText = new LocalizedString("alert." + translationKey + ".description", emptyArray);
        return alert;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Alert)) {
            return false;
        }
        Alert ao = (Alert) o;
        if (alertDescriptionText == null) {
            if (ao.alertDescriptionText != null) {
                return false;
            }
        } else {
            if (!alertDescriptionText.equals(ao.alertDescriptionText)) {
                return false;
            }
        }
        if (alertHeaderText == null) {
            if (ao.alertHeaderText != null) {
                return false;
            }
        } else {
            if (!alertHeaderText.equals(ao.alertHeaderText)) {
                return false;
            }
        }
        if (alertUrl == null) {
            return ao.alertUrl == null;
        } else {
            return alertUrl.equals(ao.alertUrl);
        }
    }

    public int hashCode() {
        return (alertDescriptionText == null ? 0 : alertDescriptionText.hashCode())
                + (alertHeaderText == null ? 0 : alertHeaderText.hashCode())
                + (alertUrl == null ? 0 : alertUrl.hashCode());
    }

    @Override
    public String toString() {
        return "Alert('"
                + (alertHeaderText != null ? alertHeaderText.toString()
                : alertDescriptionText != null ? alertDescriptionText.toString()
                : "?") + "')";
    }

    public int getHashWithoutInformedEntities() {
        return Objects.hash(alertDescriptionText, alertHeaderText, alertUrl, cause, effect, severityLevel);
    }
}
