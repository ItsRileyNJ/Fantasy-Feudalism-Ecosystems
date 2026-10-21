package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Optional;
import java.util.UUID;

/** Synchronized companion state; implemented on vanilla spiders by the mixin. */
public interface SpiderState {
    Optional<UUID> ffm$owner();
    boolean ffm$sitting();
    void ffm$setState(Optional<UUID> owner, boolean sitting);
    /** Local context for the current landing; NaN outside a landing calculation. */
    double ffm$landingDistance();
}
