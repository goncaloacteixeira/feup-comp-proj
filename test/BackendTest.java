
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BackendTest {
    @Test
    public void testHelloWorld() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run();
        assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testFindMaximum() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run();
        assertEquals("Result: 28", output.trim());
    }

    @Test
    public void testLazySort() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
        TestUtils.noErrors(result.getReports());

        // TODO check quicksort
        System.out.println(result.getJasminCode());

        String output = result.run();
    }

    // Created 10 iteration version for tests because of while(true)
    @Test
    public void testLife10itr() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Life10itr.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run();
    }

    @Test
    public void testMonteCarloPi() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run("100000");
        double pi = Integer.parseInt(output.trim().replace("Insert number: Result: ", "")) / 100.0;
        assertEquals(3.14, pi, 0.5);
    }

    @Test
    public void testQuickSort() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run();
        assertEquals(SpecsIo.getResource("fixtures/public/QuickSort.txt").trim().replace("\r\n", "\n"), output.trim().replace("\r\n", "\n"));
    }

    @Test
    public void testSimple() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testTicTacToe() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run(SpecsIo.getResource("fixtures/public/TicTacToe.input").trim().replace("\r\n", "\n"));
        assertEquals(SpecsIo.getResource("fixtures/public/TicTacToe.txt").trim().replace("\r\n", "\n").replace(":\n", ": \n"), output.trim().replace("\r\n", "\n"));

    }

    @Test
    public void testWhileAndIF() {
        JasminResult result = TestUtils.backend(SpecsIo.getResource("fixtures/public/WhileAndIF.jmm"));
        TestUtils.noErrors(result.getReports());

        String output = result.run();
        assertEquals(SpecsIo.getResource("fixtures/public/WhileAndIF.txt").trim().replace("\r\n", "\n"), output.trim().replace("\r\n", "\n"));
    }
}
