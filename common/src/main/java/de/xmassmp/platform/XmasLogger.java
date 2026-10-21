package de.xmassmp.platform;

/** Logger-Brücke zur Plattform (Fabric: SLF4J, Paper: java.util.logging, …). */
public interface XmasLogger {

    void info(String message);

    void warn(String message);

    void error(String message, Throwable error);

    /** Logger ohne Ausgabe, z. B. für Tests. */
    XmasLogger NOOP = new XmasLogger() {
        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message, Throwable error) {
        }
    };
}
