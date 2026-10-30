package de.xmassmp.event;

import de.xmassmp.event.action.EventActionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventLoaderTest {

    private static EventLoader.Result parse(String events) {
        return new EventLoader(EventActionRegistry.withDefaults()).parse("{ \"events\": [" + events + "] }");
    }

    @Test
    void stringInsteadOfDateListIsReportedAsTypeError() {
        EventLoader.Result result = parse("{ \"id\": \"xyz\", \"times\": [\"18:00\"], \"dates\": \"12-24\" }");

        assertTrue(result.events().isEmpty());
        assertEquals(1, result.errors().size(), result.errors().toString());
        String message = result.errors().get(0);
        assertTrue(message.contains("xyz"), message);
        assertTrue(message.contains("dates"), message);
        assertTrue(message.contains("Liste"), message);
        assertFalse(message.contains("ClassCastException") || message.contains("cannot be cast"), message);
    }

    @Test
    void stringInsteadOfWeekdayListIsReportedAsTypeError() {
        EventLoader.Result result = parse("{ \"id\": \"abc\", \"times\": [\"18:00\"], \"weekdays\": \"FRIDAY\" }");

        assertTrue(result.events().isEmpty());
        assertEquals(1, result.errors().size(), result.errors().toString());
        String message = result.errors().get(0);
        assertTrue(message.contains("weekdays"), message);
        assertTrue(message.contains("Liste"), message);
        assertFalse(message.contains("cannot be cast"), message);
    }
}
