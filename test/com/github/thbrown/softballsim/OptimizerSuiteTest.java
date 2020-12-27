package com.github.thbrown.softballsim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import com.github.thbrown.softballsim.optimizer.OptimizerDefinitionComposite;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.util.GsonAccessor;

public class OptimizerSuiteTest {

  /**
   * This test verifies the id property in the json for for each optimizer. The id property must meet
   * the following requirements: 1) It must be defined 2) It must be a positive whole number 3) It
   * must be unique 4) It must be the next available number (i.e no skipping numbers)
   */
  @Test
  public void optimizerIdsAreUniqueAndSerial() throws IOException {
    Set<Integer> data = new HashSet<>();

    // Get an array of all the optimizer json files
    File[] fileList = new File("./docs/definitions").listFiles();
    for (int i = 0; i < fileList.length; i++) {
      String contents = new String(Files.readAllBytes(Paths.get(fileList[i].getCanonicalPath())));
      OptimizerDefinition definition =
          GsonAccessor.getInstance().getCustom().fromJson(contents, OptimizerDefinition.class);

      // Duplicates
      try {
        if (!data.add(Integer.parseInt(definition.getId()))) {
          Assert.fail("Duplicate id detected in " + fileList[i] + ": " + definition.getId());
        }
      } catch (NumberFormatException e) {
        Assert.fail(
            "The field 'id' must be an integer for the optimizer " + fileList[i] + " but it was " + definition.getId());
      }
    }

    // Serial (starting with 0)
    for (int i = 0; i < fileList.length; i++) {
      if (!data.contains(i)) {
        Assert.fail("There are " + fileList.length + " optimizer implementations but none of them have the id: " + i);
      }
    }
  }

}
