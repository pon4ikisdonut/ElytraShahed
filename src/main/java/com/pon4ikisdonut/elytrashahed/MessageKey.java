package com.pon4ikisdonut.elytrashahed;

enum MessageKey {
    ONLY_PLAYERS("only-players"),
    NO_PERMISSION("no-permission"),
    SHAHED_ACTIVATED("shahed-activated"),
    SHAHED_DEACTIVATED("shahed-deactivated"),
    SHAHED_ALREADY_DISABLED("shahed-already-disabled"),
    SHAHED_ACTIVATE_FAIL("shahed-activate-fail"),
    SHAHED_POWER_SET("shahed-power-set"),
    SHAHED_USAGE("shahed-usage"),
    AAGUN_RECEIVED("aagun-received"),
    CONFIG_RELOADED("config-reloaded"),
    REACTIVE_ONLY_GLIDING("reactive-only-gliding"),
    REACTIVE_TRIGGERED("reactive-triggered"),
    NEED_TNT("need-tnt"),
    SHAHED_DEATH_MESSAGE("shahed-death-message");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    String path() {
        return path;
    }
}
