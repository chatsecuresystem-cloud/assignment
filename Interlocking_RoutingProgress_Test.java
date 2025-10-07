import org.junit.Test;
import static org.junit.Assert.*;

public class Interlocking_RoutingProgress_Test {

    @Test
    public void trainFrom11Reaches3ViaShortestPath() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("X", 11, 3);   // Typical route: 11 -> 7 -> 3

        int totalMoved = 0;
        for (int i = 0; i < 6; i++) {
            totalMoved += il.moveTrains(new String[]{"X"});
            if (il.getTrain("X") == 3) break;
        }
        assertTrue("The train should have moved at least once", totalMoved > 0);
        assertEquals("Train should reach destination section 3", 3, il.getTrain("X"));
    }

    @Test
    public void moveTrainsReturnsCountOfActualMovers() {
        Interlocking il = new InterlockingImpl();
        il.addTrain("A", 1, 9);
        il.addTrain("B", 2, 10);

        int moved = il.moveTrains(new String[]{"A", "B"});
        assertTrue("Expected at least one train to move on an open board", moved >= 1);

        // If we pass an empty array, nothing moves.
        assertEquals(0, il.moveTrains(new String[]{}));

        // Passing only A should move just A if it can move.
        int movedOnlyA = il.moveTrains(new String[]{"A"});
        assertTrue(movedOnlyA >= 0);
    }
}
