import org.junit.Test;
import static org.junit.Assert.*;

public class Interlocking_Junction_Crossing_Test {

    @Test
    public void crossingThroughLeftJunctionIsSerialized() {
        Interlocking il = new InterlockingImpl();
        // A: 1 -> 5 (vertical), B: 7 -> 4 (diagonal)
        il.addTrain("A", 1, 9);
        il.addTrain("B", 7, 4);

        int moved = il.moveTrains(new String[] {"A","B"});
        // Only one should get the junction in this tick
        assertEquals(1, moved);

        // One of them must still be at the start 
        int a = il.getTrain("A");
        int b = il.getTrain("B");
        boolean aStayed = (a == 1);
        boolean bStayed = (b == 7);
        assertTrue("Exactly one stayed behind", aStayed ^ bStayed);
    }
}

