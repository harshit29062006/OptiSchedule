package com.university.scheduler.util;

import com.university.scheduler.model.Faculty;
import java.util.Comparator;
import java.util.Map;

/** Sorts faculty ascending by current assigned load, for load-balancing in the optimization engine. */
public class FacultyLoadComparator implements Comparator<Faculty> {
    private final Map<Faculty, Integer> loadMap;

    public FacultyLoadComparator(Map<Faculty, Integer> loadMap) {
        this.loadMap = loadMap;
    }

    @Override
    public int compare(Faculty f1, Faculty f2) {
        return Integer.compare(loadMap.getOrDefault(f1, 0), loadMap.getOrDefault(f2, 0));
    }
}
