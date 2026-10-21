package net.itsrileynj.fantasyfeudalismmobs;

import java.util.UUID;

public interface VillagerPlayState {
    UUID ffm$playCat();
    long ffm$playUntil();
    void ffm$playCat(UUID cat,long until);
}
