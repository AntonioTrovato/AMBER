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

import org.openjdk.jmh.reconfigure.helper.HistogramItem;
import org.openjdk.jmh.reconfigure.helper.MeasurementMean;
import org.openjdk.jmh.runner.format.OutputFormat;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class ResultsManager {
    private double windowSize = 100;

    private List<MeasurementMean> allMeasurementsMeans = new ArrayList<>();
    private Map<Integer, Double> sampleInIterationMean = new HashMap<>();

    public ResultsManager() {}

    public MeasurementMean histogramToMeasurementMean(List<HistogramItem> list, boolean thrpt) {
        int fork = list.get(0).getFork();
        int iteration = list.get(0).getIteration();

        long totIt = 0;
        double sum = 0;
        for (HistogramItem item : list) {
            long count = item.getCount();
            totIt += count;
            if (thrpt)
                sum += (1/item.getValue()) * count;
            else
                sum += item.getValue() * count;
        }
        double mean = sum / totIt;

        return new MeasurementMean(fork, iteration, mean);
    }

    /**
     * This method save the result of the last iteration
     * @param list the list of results obtained until the current iteration
     */
    public void addIteration(List<HistogramItem> list, boolean thrpt) {
        int iteration = list.get(0).getIteration();
        allMeasurementsMeans.add(histogramToMeasurementMean(list,thrpt));
        sampleInIterationMean.put(iteration,histogramToMeasurementMean(list,thrpt).getAverage());
        /*System.out.println("Iteration: " + iteration + ", Mean of the current iteration histogram: " + histogramToMeasurementMean(list,thrpt).getAverage());
        System.out.println("sampleInIterationMean: " + sampleInIterationMean);*/
    }

    /**
     * This method calls the ai-base framework api to check the stability of latest measurements,
     * with a minimum of 100 iterations
     * @return true if the ai framework computed the measurements are stable, false otherwise
     */
    public boolean stableMeasurements(String localhost, String port, String model) {
        if (allMeasurementsMeans.size() < windowSize)
            return false;
        else {
            //System.out.println("I'm calling the Web Service");
            int startIndex = sampleInIterationMean.size() - 100;
            List<Double> lastMeasurements = new ArrayList<>();
            for (int i = startIndex + 1; i < sampleInIterationMean.size() + 1; i++)
                lastMeasurements.add(sampleInIterationMean.get(i));
            //System.out.println("Last 100 measurements: " + lastMeasurements);
            return makePrediction(lastMeasurements, localhost, port, model);
        }
    }

    private static boolean makePrediction(List<Double> means, String localhost, String port, String model) {
        String endpoint = "http://"+localhost+":"+port+"/predict/"+model; // Change to the appropriate endpoint

        // Manually create a JSON array string
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < means.size(); i++) {
            jsonBuilder.append(means.get(i));
            if (i < means.size() - 1) {
                jsonBuilder.append(", "); // Add a comma between elements
            }
        }
        jsonBuilder.append("]"); // Close the JSON array
        String jsonInputString = jsonBuilder.toString();

        //System.out.println("JSON input string: "+jsonInputString);

        try {
            // Create URL object
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Write the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }catch (ConnectException e) {
                System.out.println("CONNECTION ERROR, WRONG DYNAMIC HALT API HOST OR PORT");
            }

            // Read the response
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
            }

            // Parse the JSON response
            String jsonResponse = response.toString();
            //System.out.println("JSON response string: "+jsonResponse);
            // Extract the value of "steady"
            return parseSteadyValue(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean parseSteadyValue(String jsonResponse) {
        try {
            // Remove the curly braces and split the string
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("{") && jsonResponse.endsWith("}")) {
                jsonResponse = jsonResponse.substring(1, jsonResponse.length() - 1);
            }

            // Split the response to find "steady"
            String[] keyValuePairs = jsonResponse.split(",");
            for (String pair : keyValuePairs) {
                String[] entry = pair.split(":");
                if (entry[0].trim().equals("\"steady\"")) {
                    return Boolean.parseBoolean(entry[1].trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Default to false if parsing fails
    }

}