import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

/** Compare the trajectory solver with a saved pre-change class, without running a game server. */
public final class BallisticsBenchmark {
    private static volatile double sink;

    private static final class BaselineLoader extends ClassLoader {
        BaselineLoader() { super(BallisticsBenchmark.class.getClassLoader()); }
        Class<?> loadBaseline(Path path) throws Exception {
            byte[] bytes=Files.readAllBytes(path);
            return defineClass("net.itsrileynj.fantasyfeudalismmobs.SkeletonBallistics",bytes,0,bytes.length);
        }
    }

    private static Method solver(Class<?> type) throws Exception {
        Method method=type.getDeclaredMethod("launch",Vec3.class,double.class,double.class);
        method.setAccessible(true);return method;
    }

    private static boolean same(double a,double b) {
        return Double.doubleToLongBits(a)==Double.doubleToLongBits(b);
    }

    private static double time(Method solver,Vec3[] targets) throws Exception {
        long start=System.nanoTime();double sum=0;
        for(int i=0;i<100_000;i++) {
            Vec3 result=(Vec3)solver.invoke(null,targets[i%targets.length],2.24,0.05);
            if(result!=null)sum+=result.x+result.y+result.z;
        }
        sink=sum;return (System.nanoTime()-start)/100_000.0;
    }

    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Supply the pre-change SkeletonBallistics.class path");
        Method before=solver(new BaselineLoader().loadBaseline(Path.of(args[0])));
        Method after=solver(Class.forName("net.itsrileynj.fantasyfeudalismmobs.SkeletonBallistics"));
        Random random=new Random(72119);
        for(int i=0;i<10_000;i++) {
            Vec3 offset=new Vec3(.1+random.nextDouble()*40,random.nextDouble()*30-15,random.nextDouble()*40-20);
            double speed=.1+random.nextDouble()*5,gravity=random.nextDouble()*.12;
            Vec3 a=(Vec3)before.invoke(null,offset,speed,gravity),b=(Vec3)after.invoke(null,offset,speed,gravity);
            if(a==null?b!=null:b==null||!same(a.x,b.x)||!same(a.y,b.y)||!same(a.z,b.z))
                throw new AssertionError("Trajectory changed at case "+i+": "+a+" -> "+b);
        }
        System.out.println("10,000 seeded trajectories match bit for bit (including unreachable targets)");
        Vec3[] targets=new Vec3[128];
        for(int i=0;i<targets.length;i++)targets[i]=new Vec3(1+i%35,i%11-5,i%7-3);
        double[] oldTimes=new double[6],newTimes=new double[6];
        for(int round=0;round<8;round++) {
            double oldTime,newTime;
            if(round%2==0){oldTime=time(before,targets);newTime=time(after,targets);}
            else{newTime=time(after,targets);oldTime=time(before,targets);}
            System.out.printf("round=%d baseline_ns=%.1f optimized_ns=%.1f%n",round,oldTime,newTime);
            if(round>=2){oldTimes[round-2]=oldTime;newTimes[round-2]=newTime;}
        }
        Arrays.sort(oldTimes);Arrays.sort(newTimes);
        double oldMedian=(oldTimes[2]+oldTimes[3])/2,newMedian=(newTimes[2]+newTimes[3])/2;
        System.out.printf("Median after warmup: %.1f -> %.1f ns/solve (%.1f%% reduction)%n",
                oldMedian,newMedian,100*(oldMedian-newMedian)/oldMedian);
        System.out.println("This is a solver microbenchmark, not a server TPS measurement.");
    }
}
