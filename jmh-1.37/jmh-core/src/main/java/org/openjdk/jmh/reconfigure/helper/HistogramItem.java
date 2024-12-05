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
package org.openjdk.jmh.reconfigure.helper;

public class HistogramItem {
    private final int fork;
    private final int iteration;
    private final double value;
    private final long count;

    public HistogramItem(int fork, int iteration, double value, long count) {
        this.fork = fork;
        this.iteration = iteration;
        this.value = value;
        this.count = count;
    }

    public int getFork() {
        return fork;
    }

    public int getIteration() {
        return iteration;
    }

    public double getValue() {
        return value;
    }

    public long getCount() {
        return count;
    }

    public HistogramItem single() {
        return new HistogramItem(fork, iteration, value, 1);
    }

    @Override
    public String toString() {
        return "HistogramItem{" +
                "fork=" + fork +
                ", iteration=" + iteration +
                ", value=" + value +
                ", count=" + count +
                '}';
    }
}
