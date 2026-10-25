package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;

/** Server-thread-only crop search budget. Values never retain their world keys. */
public final class MouseWorkBudget {
    private static final Map<ServerLevel,Budget> LEVELS=new WeakHashMap<>();
    private static final class Budget {long tick=Long.MIN_VALUE;int searches;}
    public static boolean tryCropSearch(ServerLevel level){
        Budget budget=LEVELS.computeIfAbsent(level,key->new Budget());
        long tick=level.getGameTime();if(tick!=budget.tick){budget.tick=tick;budget.searches=0;}
        if(budget.searches>=2)return false;
        budget.searches++;return true;
    }
    private MouseWorkBudget(){}
}
