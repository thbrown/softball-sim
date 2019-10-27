package com.github.thbrown.softballsim;

import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Test different combinations of command line flags behave as expected
 */
public class CliFlagTest {
	
  @Test
  public void testNoArgumentsPrintsHelp() throws Exception {
	  try {
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();		
		System.setOut(new PrintStream(outContent));
		SoftballSim.main(new String[] {});
		String outputNoWhitespace = outContent.toString().replaceAll("\\s+","");
		String expectedHeaderNoWhitespace = CommandLineOptions.HELP_HEADER_1.replaceAll("\\s+","");
		String expectedFooterNoWhitespace = CommandLineOptions.HELP_FOOTER.replaceAll("\\s+","");

		assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedHeaderNoWhitespace));
		assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedFooterNoWhitespace));
	  } finally {
		  System.setOut(System.out);
	  }
  }
  
  @Test
  public void testHelpFlagWithOptimizerPrintsHelp() throws Exception {
	  try {
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();		
		System.setOut(new PrintStream(outContent));
		SoftballSim.main(new String[] {"--help", "-o", "0"});
		String outputNoWhitespace = outContent.toString().replaceAll("\\s+","");
		String expectedHeaderNoWhitespace = CommandLineOptions.HELP_HEADER_2.replaceAll("\\s+","");
		String expectedFooterNoWhitespace = CommandLineOptions.HELP_FOOTER.replaceAll("\\s+","");

		assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedHeaderNoWhitespace));
		assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedFooterNoWhitespace));
	  } finally {
		  System.setOut(System.out);
	  }
  }
  
  @Test
  public void testHelpFlagAfterOptionalArgsPrintsHelp() throws Exception {
	  try {
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();		
		System.setOut(new PrintStream(outContent));
		SoftballSim.main(new String[] { "-o", "0", "-s", "NETWORK", "-l" , "127.0.0.1", "-i", "0000000000", "--help", "-c"});
		String outputNoWhitespace = outContent.toString().replaceAll("\\s+","");
		String expectedHeaderNoWhitespace = CommandLineOptions.HELP_HEADER_2.replaceAll("\\s+","");
		String expectedFooterNoWhitespace = CommandLineOptions.HELP_FOOTER.replaceAll("\\s+","");

		assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedHeaderNoWhitespace));
		assertThat(outputNoWhitespace, CoreMatchers.containsString(expectedFooterNoWhitespace));
	  } finally {
		  System.setOut(System.out);
	  }
  }
  
  @Test
  public void testOptimizerFlagIsRequired() throws Exception {
	  try {
		SoftballSim.main(new String[] {"-s", "FILE_SYSTEM"});
	  } catch (MissingArgumentException e){
		assertThat(e.getMessage(), CoreMatchers.containsString("Optimizer (-o) is a required flag."));
	  }
  }
  
  @Test
  public void testInvalidFlagThrowsError() throws Exception {
	  try {
		SoftballSim.main(new String[] {"-o", "0", "--pizza"});
	  } catch (UnrecognizedOptionException e){
		assertThat(e.getMessage(), CoreMatchers.containsString("Unrecognized option:"));
	  }
  }
  

}
