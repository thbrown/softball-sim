package com.github.thbrown.softballsim;

import static org.junit.Assert.assertThat;
import java.io.ByteArrayOutputStream;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import com.github.thbrown.softballsim.helpers.TestUtil;

/**
 * Test different combinations of command line flags behave as expected
 */
public class CliFlagTest {

  @Test
  public void testNoArgumentsPrintsHelp() throws Exception {
    try {
      ByteArrayOutputStream outContent = TestUtil.redirectStdOut();
      SoftballSim.main(new String[] {});

      // Help text is formatted by the Apache CLI for maximum line length so the exact strings
      // don't always match. We are comparing strings here sans whitespace to work around that issue
      String outputNoWhitespace = outContent.toString().replaceAll("\\s+", "");
      String expectedHeaderNoWhitespace = CommandLineOptions.HELP_HEADER_1.replaceAll("\\s+", "");
      String expectedFooterNoWhitespace = CommandLineOptions.HELP_FOOTER.replaceAll("\\s+", "");

      assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedHeaderNoWhitespace));
      assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedFooterNoWhitespace));
    } finally {
      System.setOut(System.out);
    }
  }

  @Test
  public void testHelpFlagWithOptimizerPrintsHelp() throws Exception {
    try {
      ByteArrayOutputStream outContent = TestUtil.redirectStdOut();
      SoftballSim.main(new String[] {"--Help", "-O", "0"});

      // Help text is formatted by the Apache CLI for maximum line length so the exact strings
      // don't always match. We are comparing strings here sans whitespace to work around that issue
      String outputNoWhitespace = outContent.toString().replaceAll("\\s+", "");
      String expectedHeaderNoWhitespace = CommandLineOptions.HELP_HEADER_2.replaceAll("\\s+", "");
      String expectedFooterNoWhitespace = CommandLineOptions.HELP_FOOTER.replaceAll("\\s+", "");

      assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedHeaderNoWhitespace));
      assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedFooterNoWhitespace));
    } finally {
      System.setOut(System.out);
    }
  }

  @Test
  public void testHelpFlagAfterOptionalArgsPrintsHelp() throws Exception {
    try {
      ByteArrayOutputStream outContent = TestUtil.redirectStdOut();
      SoftballSim
          .main(new String[] {"-O", "0", "-S", "NETWORK", "-L", "127.0.0.1", "-I", "0000000000", "--Help", "-C"});

      // Help text is formatted by the Apache CLI for maximum line length so the exact strings
      // don't always match. We are comparing strings here sans whitespace to work around that issue
      String outputNoWhitespace = outContent.toString().replaceAll("\\s+", "");
      String expectedHeaderNoWhitespace = CommandLineOptions.HELP_HEADER_2.replaceAll("\\s+", "");
      String expectedFooterNoWhitespace = CommandLineOptions.HELP_FOOTER.replaceAll("\\s+", "");

      assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedHeaderNoWhitespace));
      assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedFooterNoWhitespace));
    } finally {
      System.setOut(System.out);
    }
  }

  @Test
  public void testOptimizerFlagIsRequired() throws Exception {
    ByteArrayOutputStream outContent = TestUtil.redirectStdOut();
    SoftballSim.main(new String[] {"-S", "FILE_SYSTEM"});
    TestUtil.asssertContainsAll(outContent.toString(), Msg.MISSING_OPTIMIZER.splitOnPlaceholders());
  }

  @Test
  public void testInvalidFlagThrowsError() throws Exception {
    ByteArrayOutputStream outContent = TestUtil.redirectStdOut();
    SoftballSim.main(new String[] {"-O", "0", "--pizza"});
    assertThat(outContent.toString(), CoreMatchers.containsString("Unrecognized option:"));
  }

}
