/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jmh.reconfigure.manager;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.util.ArrayList;
import java.util.List;

public class DynamicHaltManager extends ReconfigureManager {
    private List<IterationResult> iterationResults = new ArrayList<>();

    public DynamicHaltManager(BenchmarkParams benchParams) {
        super(benchParams);
        resultsManager = new ResultsManager();
    }

    public void addIteration(int iteration, IterationResult ir, boolean thrpt) {
        resultsManager.addIteration(toHistogramItems(0, iteration, ir), thrpt);
        iterationResults.add(ir);
        //System.out.println("Iteration results saved until now: ");
        for (IterationResult iterationResult : iterationResults) {
            //System.out.println(iterationResult.getPrimaryResult().getStatistics());
        }
    }

    public boolean checkHaltCondition(String localhost, String port, String model) {
        return resultsManager.stableMeasurements(localhost,port,model);
    }

    public List<IterationResult> getLast100IterationResults() {
        int size = iterationResults.size();
        return iterationResults.subList(Math.max(0, size - 100), size);
    }
}
