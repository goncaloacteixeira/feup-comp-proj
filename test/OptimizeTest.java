
/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.List;

public class OptimizeTest {
    private List<String> validFiles = Arrays.asList(
            "fixtures/public/FindMaximum.jmm",
            "fixtures/public/HelloWorld.jmm",
            "fixtures/public/LazySort.jmm",
            "fixtures/public/Life.jmm",
            "fixtures/public/MonteCarloPi.jmm",
            "fixtures/public/QuickSort.jmm",
            "fixtures/public/Simple.jmm",
            "fixtures/public/TicTacToe.jmm",
            "fixtures/public/WhileAndIF.jmm"
    );


    @Test
    public void testHelloWorld() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Test.jmm"));
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testJmmToOllir() {
        System.out.println("\nTesting Valid Files in test/public\n");
        for (String filename : this.validFiles) {
            System.out.printf("Testing: %-40s\n", filename);

            var result = TestUtils.optimize(SpecsIo.getResource(filename));
            TestUtils.noErrors(result.getReports());

            System.out.printf("Testing: %-40s - PASSED\n\n", filename);
        }
    }
}
