
// Interlocking_Smoke_Test.java
//
// This step will verify that InterlockingImpl can be constructed and a train can be added and queried. 

import org.junit.Test;
import static org.junit.Assert.*;

public class Interlocking_Smoke_Test {
    
    // Adding one train, then checking that getSection and getTrain reports it correctly.
    @Test
    public void canInstantiateAndAddOneTrain() {
        Interlocking system = new InterlockingImpl();
        system.addTrain("T1", 1, 4);
        assertEquals("T1", system.getSection(1));
        assertEquals(1, system.getTrain("T1"));
    }

    // Trying to add a second train into an already-occupied entry should throw an Exception.
    @Test(expected = IllegalStateException.class)
    public void cannotEnterOccupiedEntry() {
        Interlocking system = new InterlockingImpl();
        system.addTrain("T1", 1, 4);
        system.addTrain("T2", 1, 9);
    }

    // Asking for a train that doesn’t exist should throw an Exception.
    @Test(expected = IllegalArgumentException.class)
    public void unknownTrainThrows() {
        Interlocking system = new InterlockingImpl();
        system.getTrain("ghost");
    }
}
