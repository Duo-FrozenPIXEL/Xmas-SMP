package de.xmassmp.event.action;

/** Eine Aktion beim Start oder Ende eines Events. */
@FunctionalInterface
public interface EventAction {

    void run(ActionContext context);
}
