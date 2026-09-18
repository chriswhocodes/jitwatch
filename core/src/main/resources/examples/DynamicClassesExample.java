import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.LongUnaryOperator;

public class DynamicClassesExample
{
    private static final int ITERATIONS = 100_000;

    // Pre-compiled HiddenClassPayload.class (javac --release 15).
    // Implements: public long compute(long n) : triangular-number loop.
    private static final byte[] HIDDEN_CLASS_BYTES = {
        (byte)0xca,(byte)0xfe,(byte)0xba,(byte)0xbe,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x3b,
        (byte)0x00,(byte)0x10,(byte)0x0a,(byte)0x00,(byte)0x02,(byte)0x00,(byte)0x03,(byte)0x07,
        (byte)0x00,(byte)0x04,(byte)0x0c,(byte)0x00,(byte)0x05,(byte)0x00,(byte)0x06,(byte)0x01,
        (byte)0x00,(byte)0x10,(byte)0x6a,(byte)0x61,(byte)0x76,(byte)0x61,(byte)0x2f,(byte)0x6c,
        (byte)0x61,(byte)0x6e,(byte)0x67,(byte)0x2f,(byte)0x4f,(byte)0x62,(byte)0x6a,(byte)0x65,
        (byte)0x63,(byte)0x74,(byte)0x01,(byte)0x00,(byte)0x06,(byte)0x3c,(byte)0x69,(byte)0x6e,
        (byte)0x69,(byte)0x74,(byte)0x3e,(byte)0x01,(byte)0x00,(byte)0x03,(byte)0x28,(byte)0x29,
        (byte)0x56,(byte)0x07,(byte)0x00,(byte)0x08,(byte)0x01,(byte)0x00,(byte)0x12,(byte)0x48,
        (byte)0x69,(byte)0x64,(byte)0x64,(byte)0x65,(byte)0x6e,(byte)0x43,(byte)0x6c,(byte)0x61,
        (byte)0x73,(byte)0x73,(byte)0x50,(byte)0x61,(byte)0x79,(byte)0x6c,(byte)0x6f,(byte)0x61,
        (byte)0x64,(byte)0x01,(byte)0x00,(byte)0x04,(byte)0x43,(byte)0x6f,(byte)0x64,(byte)0x65,
        (byte)0x01,(byte)0x00,(byte)0x0f,(byte)0x4c,(byte)0x69,(byte)0x6e,(byte)0x65,(byte)0x4e,
        (byte)0x75,(byte)0x6d,(byte)0x62,(byte)0x65,(byte)0x72,(byte)0x54,(byte)0x61,(byte)0x62,
        (byte)0x6c,(byte)0x65,(byte)0x01,(byte)0x00,(byte)0x07,(byte)0x63,(byte)0x6f,(byte)0x6d,
        (byte)0x70,(byte)0x75,(byte)0x74,(byte)0x65,(byte)0x01,(byte)0x00,(byte)0x04,(byte)0x28,
        (byte)0x4a,(byte)0x29,(byte)0x4a,(byte)0x01,(byte)0x00,(byte)0x0d,(byte)0x53,(byte)0x74,
        (byte)0x61,(byte)0x63,(byte)0x6b,(byte)0x4d,(byte)0x61,(byte)0x70,(byte)0x54,(byte)0x61,
        (byte)0x62,(byte)0x6c,(byte)0x65,(byte)0x01,(byte)0x00,(byte)0x0a,(byte)0x53,(byte)0x6f,
        (byte)0x75,(byte)0x72,(byte)0x63,(byte)0x65,(byte)0x46,(byte)0x69,(byte)0x6c,(byte)0x65,
        (byte)0x01,(byte)0x00,(byte)0x17,(byte)0x48,(byte)0x69,(byte)0x64,(byte)0x64,(byte)0x65,
        (byte)0x6e,(byte)0x43,(byte)0x6c,(byte)0x61,(byte)0x73,(byte)0x73,(byte)0x50,(byte)0x61,
        (byte)0x79,(byte)0x6c,(byte)0x6f,(byte)0x61,(byte)0x64,(byte)0x2e,(byte)0x6a,(byte)0x61,
        (byte)0x76,(byte)0x61,(byte)0x00,(byte)0x21,(byte)0x00,(byte)0x07,(byte)0x00,(byte)0x02,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x02,(byte)0x00,(byte)0x01,
        (byte)0x00,(byte)0x05,(byte)0x00,(byte)0x06,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x09,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x1d,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x01,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x2a,(byte)0xb7,(byte)0x00,(byte)0x01,
        (byte)0xb1,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x0a,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x06,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x0b,(byte)0x00,(byte)0x0c,(byte)0x00,
        (byte)0x01,(byte)0x00,(byte)0x09,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x54,(byte)0x00,
        (byte)0x04,(byte)0x00,(byte)0x07,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x1c,(byte)0x09,
        (byte)0x42,(byte)0x0a,(byte)0x37,(byte)0x05,(byte)0x16,(byte)0x05,(byte)0x1f,(byte)0x94,
        (byte)0x9d,(byte)0x00,(byte)0x11,(byte)0x21,(byte)0x16,(byte)0x05,(byte)0x61,(byte)0x42,
        (byte)0x16,(byte)0x05,(byte)0x0a,(byte)0x61,(byte)0x37,(byte)0x05,(byte)0xa7,(byte)0xff,
        (byte)0xee,(byte)0x21,(byte)0xad,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x02,(byte)0x00,
        (byte)0x0a,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x16,(byte)0x00,(byte)0x05,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x08,(byte)0x00,(byte)0x02,(byte)0x00,(byte)0x0a,(byte)0x00,
        (byte)0x0c,(byte)0x00,(byte)0x0c,(byte)0x00,(byte)0x11,(byte)0x00,(byte)0x0a,(byte)0x00,
        (byte)0x1a,(byte)0x00,(byte)0x0f,(byte)0x00,(byte)0x0d,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0x0a,(byte)0x00,(byte)0x02,(byte)0xfd,(byte)0x00,(byte)0x05,(byte)0x04,(byte)0x04,
        (byte)0xfa,(byte)0x00,(byte)0x14,(byte)0x00,(byte)0x01,(byte)0x00,(byte)0x0e,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x02,(byte)0x00,(byte)0x0f
    };

    public static void main(String[] args) throws Throwable
    {
        runLambdaExample();
        runMethodHandleAdapterExample();

        if (sandboxJdkMajor() >= 15)
        {
            runHiddenClassExample();
        }
        else
        {
            runAnonymousClassExample();
        }
    }

    // -------------------------------------------------------------------------
    // MethodHandle adapter example : exercises combinators that force the JVM
    // to spin internal adapter/combinator classes at runtime.  These are the
    // byproduct that -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=true
    // (< JDK 21) and -Djdk.invoke.MethodHandle.dumpClassFiles (>= JDK 21)
    // capture.  Works on all JDK versions >= 8.
    //
    // asType()           -> spins a type-conversion adapter (int widened to long)
    // filterArguments()  -> spins a combinator adapter that threads each argument
    //                      through a filter handle before the target is called
    // -------------------------------------------------------------------------
    private static void runMethodHandleAdapterExample() throws Throwable
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle absLong = lookup.findStatic(Math.class, "abs",
                MethodType.methodType(long.class, long.class));

        // asType: adapter converts int argument to long before dispatch.
        MethodHandle absInt = absLong.asType(MethodType.methodType(long.class, int.class));

        // filterArguments: adapter applies absLong to both args before addExact.
        MethodHandle addExact = lookup.findStatic(Math.class, "addExact",
                MethodType.methodType(long.class, long.class, long.class));
        MethodHandle filteredAdd = MethodHandles.filterArguments(addExact, 0, absLong, absLong);

        long result = 0;

        for (int i = 1; i <= ITERATIONS; i++)
        {
            result += (long) absInt.invoke(i);
            result += (long) filteredAdd.invoke((long) -i, (long) -i);
        }

        System.out.println("MethodHandle adapter result: " + result);
    }

    // -------------------------------------------------------------------------
    // Lambda example : generates a lambda proxy class visible in the JIT log.
    //
    // The lambda body contains an inner loop so it exceeds MaxInlineSize and
    // receives its own standalone nmethod, making the assembly visible in TriView.
    // sigma(n) = sum of all proper divisors of n including 1 (excluding n itself).
    // -------------------------------------------------------------------------
    private static void runLambdaExample()
    {
        LongUnaryOperator sigma = n -> {
            long sum = 1;
            for (long d = 2; d * d <= n; d++)
            {
                if (n % d == 0)
                {
                    sum += d;
                    if (d != n / d) sum += n / d;
                }
            }
            return sum;
        };

        long result = 0;

        for (int i = 1; i <= ITERATIONS; i++)
        {
            result = sigma.applyAsLong(i);
        }

        System.out.println("Lambda result (sigma): " + result);
    }

    // -------------------------------------------------------------------------
    // JDK 15+: Lookup.defineHiddenClass via reflection.
    // Produces a FQN of the form HiddenClassPayload/0x<addr> in the JIT log.
    // -------------------------------------------------------------------------
    @SuppressWarnings({"unchecked","rawtypes"})
    private static void runHiddenClassExample() throws Throwable
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Class classOptionClass = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption");
        Object nestmate = Enum.valueOf(classOptionClass, "NESTMATE");
        Object options = Array.newInstance(classOptionClass, 1);
        Array.set(options, 0, nestmate);

        Method defineHiddenClass = MethodHandles.Lookup.class.getMethod(
                "defineHiddenClass", byte[].class, boolean.class, options.getClass());

        MethodHandles.Lookup hiddenLookup =
                (MethodHandles.Lookup) defineHiddenClass.invoke(lookup, HIDDEN_CLASS_BYTES, true, options);

        Class<?> hiddenClass = hiddenLookup.lookupClass();

        MethodHandle compute = hiddenLookup.findVirtual(
                hiddenClass, "compute", MethodType.methodType(long.class, long.class));

        Object instance = hiddenClass.getDeclaredConstructor().newInstance();

        long result = 0;

        for (int i = 0; i < ITERATIONS; i++)
        {
            result = (long) compute.invoke(instance, (long) i);
        }

        System.out.println("Hidden class result: " + result);
        System.out.println("Hidden class FQN:    " + hiddenClass.getName());
    }

    // -------------------------------------------------------------------------
    // JDK 8-14: Unsafe.defineAnonymousClass via reflection.
    // sun.misc.Unsafe is in the jdk.unsupported module which exports sun.misc
    // unconditionally, so no --add-opens flag is required.
    // JDK 11-14: produces HiddenClassPayload/0x<addr> in the JIT XML : detectable.
    // JDK 8-10:  produces bare HiddenClassPayload in the JIT XML : NOT detectable;
    //            JITWatch will not emit a hidden-class warning for these JVMs.
    // -------------------------------------------------------------------------
    private static void runAnonymousClassExample() throws Throwable
    {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        Object unsafe = theUnsafeField.get(null);

        Method defineAnonymousClass = unsafeClass.getMethod(
                "defineAnonymousClass", Class.class, byte[].class, Object[].class);

        // Patch the class file major version to match this JVM.
        // Layout: [0-3] magic, [4-5] minor version, [6-7] major version.
        // Formula: 44 + jdkMajor  (JDK 8 -> 52/0x34, JDK 14 -> 58/0x3A).
        // The payload bytecodes are basic arithmetic present since Java 1.
        byte[] classBytes = HIDDEN_CLASS_BYTES.clone();
        classBytes[6] = 0;
        classBytes[7] = (byte) (44 + sandboxJdkMajor());

        Class<?> anonClass = (Class<?>) defineAnonymousClass.invoke(
                unsafe, DynamicClassesExample.class, classBytes, null);

        Object instance = anonClass.getDeclaredConstructor().newInstance();

        // MethodHandle gives the JIT direct visibility into compute() rather than
        // routing invocations through the reflective accessor chain.
        MethodHandle compute = MethodHandles.lookup().findVirtual(
                anonClass, "compute", MethodType.methodType(long.class, long.class));

        long result = 0;

        for (int i = 0; i < ITERATIONS; i++)
        {
            result = (long) compute.invoke(instance, (long) i);
        }

        System.out.println("Anonymous class result: " + result);
        System.out.println("Anonymous class FQN:    " + anonClass.getName());
    }

    // Returns the major version of the JVM running this class (compatible with JDK 8+).
    private static int sandboxJdkMajor()
    {
        String v = System.getProperty("java.version", "1.8");

        if (v.startsWith("1."))
        {
            String[] parts = v.split("\\.");
            try { return Integer.parseInt(parts[1]); } catch (Exception ignored) {}
            return 8;
        }

        int dot = v.indexOf('.');
        try 
        { 
        	return Integer.parseInt(dot < 0 ? v : v.substring(0, dot)); 
        } 
        catch (Exception ignored) 
        {
        	// ignored; simply fall back to 8.
        }
        return 8;
    }
}
