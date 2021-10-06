package com.github.thbrown.softballsim.util;

import java.util.ArrayList;
import java.util.List;

public class CircularArray<T> {

  private final List<T> data;
  private int index = -1;
  private final int size;

  public CircularArray(int size) {
    this.size = size;
    data = new ArrayList<>(size);
  }

  public void add(T toAdd) {
    if (toAdd == null) {
      return;
    }
    index = (index + 1) % size;
    if (data.size() != this.size) {
      data.add(index, toAdd);
    } else {
      data.set(index, toAdd);
    }
  }

  /**
   * 0 - the most recently added 1 - the 2nd most recently added 2 - the 3rd most recently added
   * etc...
   * 
   * returns null if index is > size
   */
  public T get(int index) {
    if (index > size || index >= data.size()) {
      return null;
    }
    int targetIndex = mod((this.index - index), this.size);
    return data.get(targetIndex);
  }

  public int size() {
    return size;
  }

  private int mod(int a, int b) {
    return (a % b + b) % b;
  }

  /**
   * Get the earliest element added to the the array
   */
  public T earliest() {
    if (this.data.size() == 0) {
      return null;
    }
    T elem = this.get(this.size - 1);
    if (elem == null) {
      return this.data.get(0);
    }
    return elem;
  }

  public T latest() {
    return this.get(0);
  }

}
