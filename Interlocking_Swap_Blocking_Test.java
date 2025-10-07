
import org.junit.Test;
import static org.junit.Assert.*;

public class Interlocking_Swap_Blocking_Test {

    @Test
    public void headOnSwapBlocksBoth() {
        InterlockingImpl il = new InterlockingImpl();

        // A at 1 wants to go to 5; B at 5 wants to go to 1.
        il.addTrain("A", 1, 9);
        il.addTrain("B", 5, 1);

        int moved = il.moveTrains(new String[]{"A", "B"});

        // Safe interlocking:
        assertEquals("Head-on swap must be blocked for both trains", 0, moved);

        // Both remain where they were.
        assertEquals(1, il.getTrain("A"));
        assertEquals(5, il.getTrain("B"));

        // And sections still show the same occupants.
        assertEquals("A", il.getSection(1));
        assertEquals("B", il.getSection(5));
    }
}
