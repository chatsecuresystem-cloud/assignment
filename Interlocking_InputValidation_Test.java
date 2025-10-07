
import org.junit.Test;
import static org.junit.Assert.*;

public class Interlocking_InputValidation_Test {

    @Test(expected = IllegalArgumentException.class)
    public void addTrain_invalidEntry_low() {
        Interlocking s = new InterlockingImpl();
        s.addTrain("X", 0, 4);  // If the entry out of range is there
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTrain_invalidEntry_high() {
        Interlocking s = new InterlockingImpl();
        s.addTrain("X", 12, 4); 
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTrain_invalidDestination_low() {
        Interlocking s = new InterlockingImpl();
        s.addTrain("X", 1, 0);  // If destination out of range is there
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTrain_invalidDestination_high() {
        Interlocking s = new InterlockingImpl();
        s.addTrain("X", 1, 20); 
    }

    @Test(expected = IllegalArgumentException.class)
    public void addTrain_duplicateName_throws() {
        Interlocking s = new InterlockingImpl();
        s.addTrain("T1", 1, 4);
        s.addTrain("T1", 3, 8); // If same name again occurs 
    }

    @Test
    public void getSection_empty_isNull() {
        Interlocking s = new InterlockingImpl();
        assertNull(s.getSection(5));    // For empty section to be null
    }

    @Test
    public void moveTrains_null_isSafe() {
        Interlocking s = new InterlockingImpl();
        assertEquals(0, s.moveTrains(null)); 
    }

    @Test
    public void moveTrains_empty_isSafe() {
        Interlocking s = new InterlockingImpl();
        assertEquals(0, s.moveTrains(new String[0]));
    }
}
