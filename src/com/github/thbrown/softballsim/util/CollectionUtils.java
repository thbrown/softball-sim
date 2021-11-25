package com.github.thbrown.softballsim.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Set;

public class CollectionUtils {

  // https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
  public static <T> T[] concatenate(T[] a, T[] b) {
    int aLen = a.length;
    int bLen = b.length;

    @SuppressWarnings("unchecked")
    T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);

    return c;
  }

  public static String[] flattenMap(Map<String, String> args) {
    List<String> flat = new ArrayList<>();
    for (Entry<String, String> e : args.entrySet()) {
      flat.add(e.getKey());
      flat.add(e.getValue());
    }
    return flat.stream().toArray(String[]::new);
  }

  public static void renameMapKey(Map<String, String> args, String oldKey, String newKey) {
    args.put(newKey, args.get(oldKey));
    args.remove(oldKey);
    return;
  }

  public static <T> Set<T> findDuplicates(Collection<T> collection) {
    Set<T> duplicates = new LinkedHashSet<>();
    Set<T> uniques = new HashSet<>();
    for (T t : collection) {
      if (!uniques.add(t)) {
        duplicates.add(t);
      }
    }
    return duplicates;
  }
}
