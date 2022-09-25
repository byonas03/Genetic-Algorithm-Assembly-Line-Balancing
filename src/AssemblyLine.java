import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class AssemblyLine {
	LinkedList<Workstation> stations = new LinkedList<>();
	HashMap<Integer, Integer> task_times = new HashMap<>();
	LinkedList<Integer[]> precedence_relations = new LinkedList<>();
	int cycle_time;
	
	public AssemblyLine(int station_min, HashMap<Integer, Integer> tt, LinkedList<Integer[]> conn, int c_t, boolean assign) {
		task_times = tt;
		precedence_relations = conn;
		cycle_time = c_t;
		
		//Generate random line
		if (assign) {
		int limited_random_station_count = station_min + (int)(Math.random() * (0));
		for (int i = 0; i < limited_random_station_count; i++)
			stations.add(new Workstation());
			//Assign tasks randomly
		for (int i = 0; i < task_times.size(); i++) {
			int random_index = (int)(Math.random() * stations.size());
			stations.get(random_index).assigned_tasks_times.put(new Integer(i + 1), tt.get(i + 1));
		}
		for (int i = 0; i < stations.size(); i++) {
			stations.get(i).assigned_task_indicies.addAll(stations.get(i).assigned_tasks_times.keySet());
			Collections.sort(stations.get(i).assigned_task_indicies);
			stations.get(i).updateIdleTime(cycle_time);
		}
		}
	}

	@Override
	public String toString() {
		String ret = "|";
		for (int i = 0; i < stations.size(); i++) {
			Collections.sort(stations.get(i).assigned_task_indicies);
			for (int j = 0; j < stations.get(i).assigned_task_indicies.size(); j++) {
				ret += stations.get(i).assigned_task_indicies.get(j) + ",";
			}
			ret += "|";
		}
		return ret + "|";
	}
}
