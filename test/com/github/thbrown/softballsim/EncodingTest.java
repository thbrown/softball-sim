package com.github.thbrown.softballsim;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import com.github.thbrown.softballsim.util.StringUtils;

public class EncodingTest {

  @Test
  public void roundTrip() throws Exception {
    final String escapedChar = String.valueOf(Character.toChars(2588));
    final String literalChar = "█";

    System.out.println("LITERAL " + literalChar);
    ByteBuffer data = StandardCharsets.UTF_8.encode(literalChar);
    StringUtils.printBinary(data.array());

    System.out.println("ESCAPED " + escapedChar);
    ByteBuffer moreData = StandardCharsets.UTF_8.encode(escapedChar);
    StringUtils.printBinary(moreData.array());

    System.out.println("Default Charset=" + java.nio.charset.Charset.defaultCharset());
    System.out.println("\u2588");

    PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
    writer.println(literalChar);
    writer.println(escapedChar);
    writer.println("\u2588");
    writer.close();
  }

}
