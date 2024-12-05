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
import org.openjdk.jmh.reconfigure.helper.HistogramItem;
import org.openjdk.jmh.results.IterationResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class ReconfigureManager {
    protected final BenchmarkParams benchParams;

    protected ResultsManager resultsManager;

    public ReconfigureManager(BenchmarkParams benchParams) {
        this.benchParams = benchParams;
    }

    protected List<HistogramItem> toHistogramItems(int fork, int iteration, IterationResult ir) {
        List<HistogramItem> list = new ArrayList<>();
        Iterator<Map.Entry<Double, Long>> iterator = ir.getPrimaryResult().getStatistics().getRawData();
        while (iterator.hasNext()) {
            Map.Entry<Double, Long> entry = iterator.next();
            list.add(new HistogramItem(fork, iteration, entry.getKey(), entry.getValue()));
        }
        //System.out.println("Histogram of the CURRENT iteration: " + list);
        return list;
    }
}
