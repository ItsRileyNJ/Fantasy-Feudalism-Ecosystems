package net.itsrileynj.fantasyfeudalismmobs;

/** Server-owned wool progress and synchronized cold state on vanilla sheep. */
public interface SheepState {
    boolean ffm$isCold();
    int ffm$grazes();
}
