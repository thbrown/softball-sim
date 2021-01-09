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
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerDefinitionComposite;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinitionOption;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

public class OptimizerDefinitionsTest {

  @Test
  public void validateOptimizerDefinitions() throws IOException {
    Set<Integer> ids = new HashSet<>();
    Set<String> names = new HashSet<>();

    // Get an array of all the optimizer json files
    File[] fileList = new File("./docs/definitions").listFiles();
    for (int i = 0; i < fileList.length; i++) {
      File file = fileList[i];

      // Skip the README
      if (file.getName().equals("README.md")) {
        continue;
      }

      // Get the file's content
      String contents = new String(Files.readAllBytes(Paths.get(fileList[i].getCanonicalPath())));
      OptimizerDefinition definition;
      try {
        definition = GsonAccessor.getInstance().getCustom().fromJson(contents, OptimizerDefinition.class);
      } catch (Exception e) {
        throw new RuntimeException("There was an error while parsing json definition file " + fileList[i], e);
      }

      // Verify that the ids are unique and numerical integers
      try {
        if (!ids.add(Integer.parseInt(definition.getId()))) {
          Assert.fail("Duplicate id detected in " + fileList[i] + ": " + definition.getId());
        }
      } catch (NumberFormatException e) {
        Assert.fail(
            "The field 'id' must be an integer for the optimizer " + fileList[i] + " but it was " + definition.getId());
      }

      // Verify that id in the file matches the file name
      Assert.assertEquals("The id in the file name does not match the id specified inside the file", definition.getId(),
          file.getName().replaceAll(".json$", ""));

      // Verify names are present and unique
      Assert.assertNotNull("Field 'name' must not be null for optimizer definition " + file.getName(),
          definition.getName());
      if (!names.add(definition.getName())) {
        Assert.fail("Duplicate name detected in " + fileList[i] + ": " + definition.getName());
      }

      // Verify image url is present
      Assert.assertNotNull("Field 'img' must not be null for optimizer definition " + file.getName(),
          definition.getImageUrl());

      // Verify shortDescription is present
      Assert.assertNotNull("Field 'shortDescription' must not be null for optimizer definition " + file.getName(),
          definition.getShortDescription());

      // Verify longDescription is present
      Assert.assertNotNull("Field 'longDescriptionFile' must not be null for optimizer definition " + file.getName(),
          definition.getLongDescriptionFile());

      // Verify supportedLineupTypes is present, contains at least one entry, and contains only valid
      // values
      Assert.assertNotNull("Field 'supportedLineupTypes' must not be null for optimizer definition " + file.getName(),
          definition.getSupportedLineupTypes());
      Assert.assertTrue("Field 'supportedLineupTypes' must contains at least one element",
          definition.getSupportedLineupTypes().size() > 0);
      for (String lineupType : definition.getSupportedLineupTypes()) {
        Assert.assertNotNull(
            "Field 'supportedLineupTypes' contains an invalid value: " + lineupType + " in optimizer definition "
                + file.getName() + ". Valid lineup values are " + java.util.Arrays.asList(LineupTypeEnum.values()),
            LineupTypeEnum.getEnumFromName(lineupType));
      }

      // Verify options is present
      Assert.assertNotNull("Field 'options' must not be null for optimizer definition " + file.getName(),
          definition.getOptions());

      // Verify all options define the required fields
      for (OptimizerDefinitionOption option : definition.getOptions()) {

        Assert.assertNotNull(
            "Field 'longLable' must not be null for any options in optimizer definition " + file.getName(),
            option.getLongLabel());
        Assert.assertNotNull("Field 'shortLable' must not be null for option " + option.getLongLabel()
            + " in optimizer definition " + file.getName(), option.getShortLabel());
        Assert.assertNotNull("Field 'description' must not be null for option " + option.getLongLabel()
            + " in optimizer definition " + file.getName(), option.getDescription());
        Assert.assertNotNull("Field 'type' must not be null for option " + option.getLongLabel()
            + " in optimizer definition " + file.getName(), option.getType());

        if (option.getType() == "Enumeration") {
          Assert.assertNotNull("For the Enumeration option type, field 'values' must not be null for option "
              + option.getLongLabel() + " in optimizer definition " + file.getName(), option.getType());
        }
      }
    }

    // Verify Ids are Serial (starting with 0)
    for (int i = 0; i < (fileList.length - 1); i++) {
      if (!ids.contains(i)) {
        Assert.fail("There are " + fileList.length + " optimizer implementations but none of them have the id: " + i);
      }
    }
  }

}
