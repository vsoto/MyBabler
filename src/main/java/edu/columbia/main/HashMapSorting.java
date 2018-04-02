package edu.columbia.main;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class HashMapSorting{

public static Set<Entry<String, Double>> sort(HashMap<String,Double> hm) {
	Set<Entry<String, Double>> entries = hm.entrySet();
	
	Comparator<Entry<String, Double>> valueComparator = new Comparator<Entry<String, Double>>() {
    	@Override
    	public int compare(Entry<String, Double> e1, Entry<String, Double> e2) {
        	Double v1 = e1.getValue();
        	Double v2 = e2.getValue();
        	return v2.compareTo(v1);
    	}
	};

	// Sort method needs a List, so let's first convert Set to List in Java
	List<Entry<String, Double>> listOfEntries = new ArrayList<Entry<String, Double>>(entries);
	// sorting HashMap by values using comparator
	Collections.sort(listOfEntries, valueComparator);
	LinkedHashMap<String, Double> sortedByValue = new LinkedHashMap<String, Double>(listOfEntries.size());
	// copying entries from List to Map
	for(Entry<String, Double> entry : listOfEntries) {
		sortedByValue.put(entry.getKey(), entry.getValue());
	}
	Set<Entry<String, Double>> entrySetSortedByValue = sortedByValue.entrySet();
	return entrySetSortedByValue;
}

}





















































