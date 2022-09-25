import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.swing.JFrame;

public class GenInterface {
	public String filename;
	public int tasks = 0, cycle_time = 0, optimal_stations_count, total_task_time;
	public double order_strength = 0, mutation_rate = 0.0;
	public HashMap<Integer, Integer> task_times = new HashMap<>();
	public LinkedList<Integer[]> precedence_relations = new LinkedList<>();

	public int current_generation = 0;
	public long start_time = System.currentTimeMillis(), convergence_time = 0;
	public LinkedList<AssemblyLine> population = new LinkedList<>();
	public boolean hybridization;
	public int null_clock = 0;

	//Graphics
	public LinkedList<Double> x_positions = new LinkedList<>(), best_fitnesses = new LinkedList<>(), worst_fitnesses = new LinkedList<>(), average_fitnesses = new LinkedList<>();

	public GenInterface(String fn, int population_size, double m_r, boolean hyb) {
		mutation_rate = m_r;
		filename = fn;
		hybridization = hyb;
		//Compile and read in file data
		Scanner sc = null;
		try {
			sc = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.equals("<number of tasks>")) {
				tasks = Integer.parseInt(sc.nextLine());
			} else if (line.equals("<cycle time>")) {
				cycle_time = Integer.parseInt(sc.nextLine());
			} else if (line.equals("<order strength>")) {
				order_strength = Double.parseDouble(sc.nextLine().replaceAll(",", "."));
			} else if (line.equals("<task times>")) {
				for (int i = 0; i < tasks; i++) 
					task_times.put(sc.nextInt(), sc.nextInt());
			} else if (line.equals("<precedence relations>")) {
				String cur = sc.nextLine();
				while (cur.length() > 1) {
					String[] split = cur.split(",");
					Integer[] newsplit = new Integer[split.length];
					for (int i = 0; i < split.length; i++)
						newsplit[i] = Integer.parseInt(split[i]);
					precedence_relations.add(newsplit);
					cur = sc.nextLine();
				}
			}
		}
		for (int i = 0; i < task_times.size(); i++)
			total_task_time += task_times.get(i + 1);

		optimal_stations_count = getOptimalStationCount();

		//Generate population of assembly lines
		while (population.size() < population_size) {
			AssemblyLine al = new AssemblyLine(optimal_stations_count, task_times, precedence_relations, cycle_time, true);
			if (true) 
				population.add(al);
		}
		best_fitnesses.add(fitness(getFittestPopulationMember(), false));
		worst_fitnesses.add(fitness(getLeastFittestPopulationMember(), false));
		average_fitnesses.add(getAverageFitness());
	}

	public void procedeGenerations() {
		try {
			//Hybridization stopping rule
			null_clock = 0;
			if (getPrecedenceViolationsProportion(getFittestPopulationMember()) == 0.0 && getExceedingIdleProportion(getFittestPopulationMember()) == 0.0 && convergence_time == 0) {
				convergence_time = System.currentTimeMillis();
			}
			//Reproducing high fitness population members
			LinkedList<AssemblyLine> offspring = new LinkedList<>();
			while (offspring.size() < population.size()) {
				AssemblyLine[] parent_set = selectParents();
				if (parent_set != null && !parent_set[0].equals(parent_set[1])) {
					offspring.add(reproduce(parent_set[0], parent_set[1]));
				} else {
					null_clock++;
				}
			}

			//Merging new and old generation
			LinkedList<AssemblyLine> next_generation = new LinkedList<>();
			next_generation.addAll(offspring);
			next_generation.addAll(population);

			//Mutating (mutation_rate)% of population members
			for (int j = 0; j < next_generation.size(); j++)
				if (Math.random() < mutation_rate)
					next_generation.set(j, mutate(next_generation.get(j)));

			//Updating idle times
			for (int j = 0; j < next_generation.size(); j++) {
				for (int x = 0; x < next_generation.get(j).stations.size(); x++) {
					next_generation.get(j).stations.get(x).updateIdleTime(cycle_time);
				}
			}

			//Sorting fitnesses
			TreeMap<Double, Integer> fitness_index = new TreeMap<>();
			for (int j = 0; j < next_generation.size(); j++) 
				fitness_index.put(fitness(next_generation.get(j), false) + (Math.random() * .005), j);
			LinkedList<AssemblyLine> sorted_next_generation = new LinkedList<>();
			LinkedList<Integer> value_list = new LinkedList<>();
			value_list.addAll(fitness_index.values());
			for (int j = 0; j < fitness_index.values().size(); j++) {
				sorted_next_generation.add(next_generation.get(value_list.get(j)));
			}
			next_generation.clear();
			next_generation.addAll(sorted_next_generation);

			//Remove bottom 50th percentile
			while (next_generation.size() > population.size())
				next_generation.remove(0);
			population.clear();
			population.addAll(next_generation);
			//System.out.println(population);
			//System.out.println(getFittestPopulationMember());
			//System.out.println("Fittest: " + fitness(getFittestPopulationMember(), true));
			current_generation++;
			best_fitnesses.add(fitness(getFittestPopulationMember(), false));
			worst_fitnesses.add(fitness(getLeastFittestPopulationMember(), false));
			average_fitnesses.add(getAverageFitness());
			UserInterface.paint(UserInterface.f.getGraphics());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Assess fitness of assembly line based on efficiency, idle time / over time, station complexity, precedence relationship violations
	public double fitness(AssemblyLine al, boolean diagnostics) {
		double delta_time = 0;
		double station_proportion = (double)optimal_stations_count / al.stations.size(), precedence_violations_proportion = 0.0;

		//Calculation of total precedence violation occurrences
		LinkedList<Workstation> stations = al.stations;
		for (int i = 0; i < precedence_relations.size(); i++) {
			int a = precedence_relations.get(i)[0], b = precedence_relations.get(i)[1];
			int first_pos = -1, second_pos = -1;
			for (int j = 0; j < stations.size(); j++) {
				for (int c = 0; c < stations.get(j).assigned_task_indicies.size(); c++) {
					if (stations.get(j).assigned_task_indicies.get(c) == a)
						first_pos = j;
					if (stations.get(j).assigned_task_indicies.get(c) == b)
						second_pos = j;
				}
			}
			if (first_pos > second_pos)
				precedence_violations_proportion++;
		}
		precedence_violations_proportion /= precedence_relations.size();

		//Calculation of weighted delta time
		for (int i = 0; i < al.stations.size(); i++) {
			if (al.stations.get(i).idle_time < 0)
				delta_time += Math.abs(al.stations.get(i).idle_time) * 2;
			else
				delta_time += Math.abs(al.stations.get(i).idle_time);
		}

		//Calculation of line efficiency
		int lead_time = cycle_time * al.stations.size();
		double efficiency = (double)total_task_time / lead_time;

		//Normalizing delta_time via sigmoidal function
		double delta_time_sigmoid = (1 / (1 + Math.pow(Math.E, -(delta_time / 1000))));
		if (diagnostics) {
			//System.out.println(precedence_violations_proportion + "/" + station_proportion +  "/" + efficiency + "/" + delta_time + "/" + total_task_time);
			for (int i = 0; i < al.stations.size(); i++) {
				//System.out.print(al.stations.get(i).idle_time + " ");
			} 
		}
		return 1 - (precedence_violations_proportion) - (getExceedingIdleProportion(al) / 3) - (getValidIdleProportion(al) / 100);
	}

	public double getPrecedenceViolationsProportion(AssemblyLine al) {
		double precedence_violations_proportion = 0.0;
		LinkedList<Workstation> stations = al.stations;
		for (int i = 0; i < precedence_relations.size(); i++) {
			int a = precedence_relations.get(i)[0], b = precedence_relations.get(i)[1];
			int first_pos = -1, second_pos = -1;
			for (int j = 0; j < stations.size(); j++) {
				for (int c = 0; c < stations.get(j).assigned_task_indicies.size(); c++) {
					if (stations.get(j).assigned_task_indicies.get(c) == a)
						first_pos = j;
					if (stations.get(j).assigned_task_indicies.get(c) == b)
						second_pos = j;
				}
			}
			if (first_pos > second_pos)
				precedence_violations_proportion++;
		}
		precedence_violations_proportion /= precedence_relations.size();
		return precedence_violations_proportion;
	}

	public double getExceedingIdleProportion(AssemblyLine al) {
		double delta_time = 0.0;
		for (int i = 0; i < al.stations.size(); i++) {
			if (al.stations.get(i).idle_time < 0)
				delta_time += Math.abs(al.stations.get(i).idle_time);
			else
				delta_time += Math.abs(al.stations.get(i).idle_time) * 0;
		}
		return delta_time / (al.stations.size() * cycle_time);
	}

	public double getValidIdleProportion(AssemblyLine al) {
		double delta_time = 0.0;
		for (int i = 0; i < al.stations.size(); i++) {
			if (al.stations.get(i).idle_time < 0)
				delta_time += Math.abs(al.stations.get(i).idle_time) * 0;
			else
				delta_time += Math.abs(al.stations.get(i).idle_time);
		}
		return delta_time / (al.stations.size() * cycle_time);
	}

	//Tournament/Roulette; compare fitnesses
	public AssemblyLine[] selectParents() {
		AssemblyLine first_parent, second_parent;

		int first_index_first_tournament = (int)(Math.random() * population.size());
		int second_index_first_tournament = (int)(Math.random() * population.size());

		if (fitness(population.get(first_index_first_tournament), false) >= fitness(population.get(second_index_first_tournament), false)) {
			first_parent = population.get(first_index_first_tournament);
		} else {
			first_parent = population.get(second_index_first_tournament);
		}

		int first_index_second_tournament = (int)(Math.random() * population.size());
		int second_index_second_tournament = (int)(Math.random() * population.size());

		if (fitness(population.get(first_index_second_tournament), false) >= fitness(population.get(second_index_second_tournament), false)) {
			second_parent = population.get(first_index_second_tournament);
		} else {
			second_parent = population.get(second_index_second_tournament);
		}
		if (!first_parent.toString().equals(second_parent.toString())) {
			return new AssemblyLine[] {first_parent, second_parent};
		} else
			return null;
	}

	//Crossover data for chosen lines and crossover stations within each
	public AssemblyLine reproduce(AssemblyLine modifier, AssemblyLine base) {


		/* Switch specific genes methods
		for (int x = 0; x < 20; x++) {
			int switch_index = (int)(Math.random() * task_times.size()) + 1;
			int modifier_switch_index = 0, base_switch_index = 0;

			for (int i = 0; i < modifier.stations.size(); i++) {
				if (modifier.stations.get(i).assigned_task_indicies.contains(switch_index))
					modifier_switch_index = i;
			}
			for (int i = 0; i < base.stations.size(); i++) {
				if (base.stations.get(i).assigned_task_indicies.contains(switch_index))
					base_switch_index = i;
			}

			if (modifier_switch_index < base.stations.size() && modifier_switch_index != base_switch_index) {
				base.stations.get(modifier_switch_index).assigned_task_indicies.add(switch_index);
				base.stations.get(modifier_switch_index).assigned_tasks_times.put(switch_index, task_times.get(switch_index));
				base.stations.get(base_switch_index).assigned_task_indicies.remove((Integer)switch_index);
				base.stations.get(base_switch_index).assigned_tasks_times.remove((Integer)switch_index);
			}
		}
		return base;
		 */

		//Choose each task location based on randomly chosen station's location
		AssemblyLine child = new AssemblyLine(optimal_stations_count, task_times, precedence_relations, cycle_time, false);
		AssemblyLine[] parent_set = new AssemblyLine[] {modifier, base};
		int stations_amount = parent_set[(int)(Math.random() * 2)].stations.size();
		for (int i = 0; i < stations_amount; i++) {
			child.stations.add(new Workstation());
		}
		for (int i = 0; i < task_times.size(); i++) {
			int chosen_index = (int)(Math.random() * 2);
			AssemblyLine chosen = parent_set[chosen_index];
			int n = i + 1;
			int station_index = -1;
			for (int j = 0; j < chosen.stations.size(); j++) {
				if (chosen.stations.get(j).assigned_task_indicies.contains(n)) {
					station_index = j;
					break;
				}
			}
			if (station_index < child.stations.size()) {
				child.stations.get(station_index).assigned_task_indicies.add(n);
				child.stations.get(station_index).assigned_tasks_times.put(n, task_times.get(n));
			} else {
				if (chosen_index == 0) {
					for (int j = 0; j < parent_set[1].stations.size(); j++) {
						if (parent_set[1].stations.get(j).assigned_task_indicies.contains(n)) {
							station_index = j;
							break;
						}
					}
				} else {
					for (int j = 0; j < parent_set[0].stations.size(); j++) {
						if (parent_set[0].stations.get(j).assigned_task_indicies.contains(n)) {
							station_index = j;
							break;
						}
					}
				}
				child.stations.get(station_index).assigned_task_indicies.add(n);
				child.stations.get(station_index).assigned_tasks_times.put(n, task_times.get(n));
			}
		}
		for (int i = 0; i < child.stations.size(); i++) {
			Collections.sort(child.stations.get(i).assigned_task_indicies);
			child.stations.get(i).updateIdleTime(cycle_time);
		}
		return child;
	}

	//Perform based on 'mutation_rate.' Randomly swap random tasks along stations; alternatively, swap random tasks that are incorrect.
	public AssemblyLine mutate(AssemblyLine al) {
		if (!hybridization) {
			LinkedList<Integer> pool = new LinkedList<>();
			for (int i = 0; i < 10; i++)
				pool.add(1 + (int)(Math.random() * task_times.size()));
			for (int j = 0; j < pool.size(); j++) {
				int switch_index = pool.get(j);
				int first_index = 0;
				for (int i = 0; i < al.stations.size(); i++) {
					if (al.stations.get(i).assigned_task_indicies.contains(switch_index))
						first_index = i;
				}
				al.stations.get(first_index).assigned_task_indicies.remove((Integer)switch_index);
				al.stations.get(first_index).assigned_tasks_times.remove((Integer)switch_index);

				first_index = (int)(Math.random() * al.stations.size());
				al.stations.get(first_index).assigned_task_indicies.add(switch_index);
				al.stations.get(first_index).assigned_tasks_times.put(switch_index, task_times.get(switch_index));
			}
		} else {
			LinkedList<Integer> pool = new LinkedList<>();
			pool.addAll(getPrecedenceViolatingTasks(al));
			pool.addAll(getPrecedenceViolatingTasks(al));
			pool.addAll(getPrecedenceViolatingTasks(al));
			int x = 10;
			for (int i = 0; i < x; i++) {
				//pool.add((int)(Math.random() * task_times.size()) + 1);
			}
			for (int j = 0; j < pool.size() * .2; j++) {
				int switch_index = pool.get((int)(pool.size() * Math.random()));
				int first_index = 0;
				for (int i = 0; i < al.stations.size(); i++) {
					if (al.stations.get(i).assigned_task_indicies.contains(switch_index))
						first_index = i;
				}
				al.stations.get(first_index).assigned_task_indicies.remove((Integer)switch_index);
				al.stations.get(first_index).assigned_tasks_times.remove((Integer)switch_index);

				first_index = (int)(Math.random() * al.stations.size());
				al.stations.get(first_index).assigned_task_indicies.add(switch_index);
				al.stations.get(first_index).assigned_tasks_times.put(switch_index, task_times.get(switch_index));
			}
		}
		return al;
	}

	public LinkedList<Integer> getPrecedenceViolatingTasks(AssemblyLine al) {
		LinkedList<Integer> violating_tasks = new LinkedList<>();
		LinkedList<Workstation> stations = al.stations;
		for (int i = 0; i < precedence_relations.size(); i++) {
			int a = precedence_relations.get(i)[0], b = precedence_relations.get(i)[1];
			int first_pos = -1, second_pos = -1;
			for (int j = 0; j < stations.size(); j++) {
				for (int c = 0; c < stations.get(j).assigned_task_indicies.size(); c++) {
					if (stations.get(j).assigned_task_indicies.get(c) == a)
						first_pos = j;
					if (stations.get(j).assigned_task_indicies.get(c) == b)
						second_pos = j;
				}
			}
			if (first_pos > second_pos) {
				violating_tasks.add(a);
				violating_tasks.add(b);
			}
		}
		for (int i = 0; i < al.stations.size(); i++) {
			if (al.stations.get(i).idle_time < 0 && Math.random() < .03) {
				//violating_tasks.addAll(al.stations.get(i).assigned_task_indicies);
			}
		}
		return violating_tasks;
	}

	//Sum of task times divided by cycle time; limits time alloted to each station
	public int getOptimalStationCount() {
		int sum = 0;
		for (int i = 0; i < task_times.size(); i++)
			sum += task_times.get(i + 1);
		return (int)Math.ceil((double)sum / cycle_time);
	}

	//Returns the population member with the highest fitness score
	public AssemblyLine getFittestPopulationMember() {
		int index = -1;
		double max = Integer.MIN_VALUE;
		for (int i = 0; i < population.size(); i++) {
			if (fitness(population.get(i), false) > max) {
				max = fitness(population.get(i), false);
				index = i;
			}
		}
		return population.get(index);
	}

	//Returns the population member with the lowest fitness score
	public AssemblyLine getLeastFittestPopulationMember() {
		int index = -1;
		double min = Integer.MAX_VALUE;
		for (int i = 0; i < population.size(); i++) {
			if (fitness(population.get(i), false) < min) {
				min = fitness(population.get(i), false);
				index = i;
			}
		}
		return population.get(index);
	}

	//Returns the average fitness of the entire population
	public double getAverageFitness() {
		double sum = 0.0;
		for (int i = 0; i < population.size(); i++) {
			sum += fitness(population.get(i), false);
		}
		return sum / population.size();
	}

	//Determines the validity of an assembly line given precedence relations
	public boolean isLineValid(AssemblyLine al) {
		LinkedList<Workstation> stations = al.stations;
		for (int i = 0; i < precedence_relations.size(); i++) {
			int a = precedence_relations.get(i)[0], b = precedence_relations.get(i)[1];
			int first_pos = -1, second_pos = -1;
			for (int j = 0; j < stations.size(); j++) {
				for (int c = 0; c < stations.get(j).assigned_task_indicies.size(); c++) {
					if (stations.get(j).assigned_task_indicies.get(c) == a)
						first_pos = j;
					if (stations.get(j).assigned_task_indicies.get(c) == b)
						second_pos = j;
				}
			}
			if (first_pos > second_pos)
				return false;
		}
		return true;
	}
}
