package de.xmassmp.event.action;

import com.google.gson.JsonObject;
import de.xmassmp.event.EventConfigException;

/**
 * Ein Aktionstyp für {@code events.jsonc} ({@code "type": "<id>"}). Eigene Typen, z. B. für den
 * Christmas-Twist, werden über {@link EventActionRegistry#register(EventActionType)} ergänzt.
 */
public interface EventActionType {

    String id();

    EventAction parse(JsonObject json) throws EventConfigException;
}
