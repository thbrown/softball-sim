package com.github.thbrown.softballsim.gson;

/**
 * POJO used for deserializing players JSON from softball app.
 * 
 * Note: Should this be combined with the Player class?
 */
public class ParsedPlayerEntry {
  
  private String id;
  private String gender;
  private int outs;
  private int singles;
  private int doubles;
  private int triples;
  private int homeruns;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public int getSingles() {
    return singles;
  }

  public int getDoubles() {
    return doubles;
  }

  public int getTriples() {
    return triples;
  }

  public int getHomeruns() {
    return homeruns;
  }

  public int getOuts() {
    return outs;
  }

}
