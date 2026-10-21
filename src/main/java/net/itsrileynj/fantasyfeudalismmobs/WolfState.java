package net.itsrileynj.fantasyfeudalismmobs;

public interface WolfState {
    boolean ffm$stalking();
    void ffm$setStalking(boolean stalking);
    boolean ffm$hunting();
    void ffm$setHunting(boolean hunting);
    float ffm$crouch(float partialTick);
}
