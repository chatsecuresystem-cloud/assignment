import org.junit.Test;
import static org.junit.Assert.*;

public class Interlocking_JunctionPriority_Test {

    @Test
    public void passengerPriorityOverFreight() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("P", 7, 11); // passenger goes 7 -> 11 in one tick
        il.addTrain("F", 1, 9);  // freight may go 1 -> 5 if not conflicting

        int moved = il.moveTrains(new String[]{"P","F"});

        // At least the passenger must move; freight may also move (no conflict with P->11)
        assertTrue("Passenger should not be blocked at the junction", moved >= 1);
        assertEquals("Passenger must be in section 11 after one tick", "P", il.getSection(11));

        // Freight either waited at 1 or advanced to 5; both are acceptable
        boolean freightStayed = "F".equals(il.getSection(1));
        boolean freightAdvanced = "F".equals(il.getSection(5));
        assertTrue("Freight either stays at 1 or moves to 5 without blocking passenger",
                freightStayed || freightAdvanced);
    }

    @Test
    public void contestedSameSectionBlocksHeadOnSwap() {
        Interlocking il = new InterlockingImpl();
        // Setup a head-on swap attempt: A wants 6, B wants 2
        il.addTrain("A", 2, 6);
        il.addTrain("B", 6, 2);

        int moved = il.moveTrains(new String[]{"A","B"});

        // Engine policy: block head-on swap => 0 moved, both remain in place
        assertEquals("Head-on swap should be blocked for safety", 0, moved);
        assertEquals(2, il.getTrain("A"));
        assertEquals(6, il.getTrain("B"));
        assertEquals("A", il.getSection(2));
        assertEquals("B", il.getSection(6));
    }

    @Test
    public void trainAtDestinationDoesNotMove() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("T", 3, 3); // already at destination
        assertEquals(0, il.moveTrains(new String[]{"T"}));
        assertEquals(3, il.getTrain("T"));
    }
}
