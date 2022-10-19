package com.example.firstapp;

import android.os.Environment;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class StepCounter {

    public static int getStepCount(List<Pair<Long,Double>> dataPoints) {
        dataPoints = smoothenCurve(dataPoints, 10);
        Double mean = calculateParams(dataPoints, "mean");
        dataPoints = dataPoints.stream()
                .map(entry -> Pair.of(entry.getLeft(), entry.getRight()-mean))
                .collect(Collectors.toList());
        Double threshold = Math.max(calculateParams(dataPoints, "std") * 1.1, 0.2);
        dataPoints = getPeaks(dataPoints);
        dataPoints = filterPeaks(dataPoints, 15.0, threshold);
        return dataPoints.size();
    }

    private static List<Pair<Long,Double>> getPeaks(List<Pair<Long,Double>> points) {
        List<Pair<Long,Double>> peaks = new ArrayList<>();
        for(int i=1;i<points.size()-1;i++) {
            Double left = points.get(i-1).getRight();
            Double right = points.get(i+1).getRight();
            Double current = points.get(i).getRight();
            if(current > left && current > right) {
                peaks.add(points.get(i));
            }
        }
        return peaks;
    }

    private static List<Pair<Long,Double>> filterPeaks(List<Pair<Long,Double>> peaks, Double minDist, Double threshold) {
        List<Pair<Long,Double>> filteredPeaks = new ArrayList<>();
        final Double minHt = threshold == null ? Double.MIN_VALUE : threshold;
        Deque<Pair<Long,Double>> deq = new ArrayDeque<>();
        for (Pair<Long, Double> peak : peaks) {
            Pair<Long,Double> prev = deq.peekLast();
            long currDist =  prev == null ? Long.MAX_VALUE : Math.abs(peak.getLeft() - prev.getLeft());
            if(!deq.isEmpty() && currDist < minDist) {
                if(prev.getRight() < peak.getRight()) {
                    deq.pollLast();
                    deq.offerLast(peak);
                }
            } else {
                deq.offerLast(peak);
            }
        }
        filteredPeaks = deq.stream()
                .filter(entry -> entry.getRight() >= minHt)
                .collect(Collectors.toList());
        return filteredPeaks;
    }

    private static List<Pair<Long,Double>> smoothenCurve(List<Pair<Long,Double>> points, int window) {
        List<Pair<Long,Double>> mergedPoints = new ArrayList<>();
        for(int i=0;i<points.size();i++) {
            Double sum = points.get(i).getRight();
            int index = 1, count=0;
            while(index <= window/2) {
                sum += i-index >=0 ? points.get(i-index).getRight() : 0;
                count += i-index >=0 ? 1 : 0;
                sum += i+index < points.size() ? points.get(i+index).getRight() : 0;
                count += i+index < points.size() ? 1 : 0;
                index++;
            }
            mergedPoints.add(Pair.of(points.get(i).getLeft(), sum/count+1));
        }
        return mergedPoints;
    }

    private static Double calculateParams(List<Pair<Long,Double>> dataPoints, String op) {
        double sum = 0, mean = 0, variance = 0, deviation = 0;

        List<Double> data = dataPoints.stream()
                .map(Pair::getRight)
                .collect(Collectors.toList());

        for (Double datum : data) {
            sum+=datum;
        }
        mean = sum / data.size();
        sum = 0;
        for (Double datum : data) {
            sum = sum + Math.pow((datum - mean), 2);
        }
        variance = sum / data.size();
        deviation = Math.sqrt(variance);
        if(op == "mean")
            return mean;
        return deviation;
    }
}
