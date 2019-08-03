package com.github.thbrown.softballsim.helpers;

import com.google.gson.Gson;

public class PlayerBuilder {

  private String id;
  private String gender;
  private String outs = "0";
  private String singles = "0";
  private String doubles = "0";
  private String triples = "0";
  private String homeruns = "0";

  public PlayerBuilder withId(String id) {
    this.id = id;
    return this;
  }
  
  public PlayerBuilder withGender(String gender) {
    this.gender = gender;
    return this;
  }
  
  public PlayerBuilder withOuts(int outs) {
    this.outs = String.valueOf(outs);
    return this;
  }
  
  public PlayerBuilder withSingles(int singles) {
    this.singles = String.valueOf(singles);
    return this;
  }
  
  public PlayerBuilder withDoubles(int doubles) {
    this.doubles = String.valueOf(doubles);
    return this;
  }
  
  public PlayerBuilder withTriples(int triples) {
    this.triples = String.valueOf(triples);
    return this;
  }
  
  public PlayerBuilder withHomeruns(int homeruns) {
    this.homeruns = String.valueOf(homeruns);
    return this;
  }

  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

}
