package com.github.thbrown.softballsim;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.github.thbrown.softballsim.util.CircularArray;

public class CircularArrayTest {
  @Test
  public void testValidInsertsAndGets() throws Exception {
    Integer A = 8;
    Integer B = 6;
    Integer C = 7;
    Integer D = 5;

    CircularArray<Integer> array = new CircularArray<>(10);
    array.add(A);
    array.add(B);
    array.add(C);
    array.add(D);

    assertEquals(array.get(0), D);
    assertEquals(array.get(1), C);
    assertEquals(array.get(2), B);
    assertEquals(array.get(3), A);
    assertEquals(array.get(4), null);
    assertEquals(array.get(25), null);
  }


  @Test
  public void testValidCircularInserts() throws Exception {
    Integer A = 8;
    Integer B = 6;
    Integer C = 7;
    Integer D = 5;

    CircularArray<Integer> array = new CircularArray<>(3);
    array.add(A);
    array.add(B);
    array.add(C);
    array.add(D);

    assertEquals(array.get(0), D);
    assertEquals(array.get(1), C);
    assertEquals(array.get(2), B);
    assertEquals(array.get(3), null);
  }

}
