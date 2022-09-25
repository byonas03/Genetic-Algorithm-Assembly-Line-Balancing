import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Workstation {
	public LinkedList<Integer> assigned_task_indicies = new LinkedList<>();
	public HashMap<Integer, Integer> assigned_tasks_times = new HashMap<>();
	public int idle_time;
	
	public Workstation() {
		
	}
	
	public void updateIdleTime(int cycle_time) {
		Collections.sort(assigned_task_indicies);
		for (Entry<Integer, Integer> entry : assigned_tasks_times.entrySet()) {
            cycle_time -= entry.getValue();
		}
		idle_time = cycle_time;
	}
}
