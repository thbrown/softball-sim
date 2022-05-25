package com.github.thbrown.softballsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.helpers.TestUtil;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloMultiGameSimulationTask;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.TaskResult;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import org.junit.Test;

public class ExtrapolationTest {

  @Test
  public void gcpFunctionTest() throws IOException, InterruptedException {
    List<Long> test = new ArrayList();
    test.add(1L);
    test.add(2L);
    test.add(4L);
    test.add(8L);
    test.add(16L);
    List<Long> resultA = TestUtil.getPredictionArray(test);
    Logger.log(resultA.stream().mapToInt(Long::intValue).sum());
    test.clear();

    test.add(1L);
    test.add(1L);
    test.add(1L);
    test.add(1L);
    test.add(1L);
    List<Long> resultB = TestUtil.getPredictionArray(test);
    Logger.log(resultB.stream().mapToInt(Long::intValue).sum());
    test.clear();

    test.add(1L);
    test.add(2L);
    test.add(3L);
    test.add(4L);
    test.add(5L);
    List<Long> resultC = TestUtil.getPredictionArray(test);
    Logger.log(resultC.stream().mapToInt(Long::intValue).sum());
    test.clear();
  }



}
