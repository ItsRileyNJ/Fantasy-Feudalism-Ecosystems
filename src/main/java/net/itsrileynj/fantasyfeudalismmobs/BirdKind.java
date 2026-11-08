package net.itsrileynj.fantasyfeudalismmobs;

public enum BirdKind {
    ROBIN("robin",0,.8F,true), BLUE_JAY("blue_jay",1,.85F,false),
    GOLDFINCH("goldfinch",2,.72F,true), CROW("crow",3,1F,false);
    public final String id;
    public final int textureRow;
    public final float scale;
    public final boolean flocking;
    BirdKind(String id,int row,float scale,boolean flocking) { this.id=id;textureRow=row;this.scale=scale;this.flocking=flocking; }
}
