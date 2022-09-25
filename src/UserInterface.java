import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

public class UserInterface {
	static JFrame f = new JFrame("User Interface | Genetic Algortihm ALBP");
	static LinkedList<GenInterface> gi_list = new LinkedList<>();
	static LinkedList<Color> color_wheel = new LinkedList<>();
	static boolean comparison_mode;
	static int hover_index = -1, preferred_index = -1;
	public boolean pause = false, failed = false, ignore = false;
	static long last_comparison_call = 0;
	int max, file_index, active_lines;
	FileWriter writer;

	public UserInterface(boolean c_m, int m, int f_i) throws IOException {
		comparison_mode = c_m;
		writer = new FileWriter("results.txt", true);
		file_index = f_i;
		max = m;
		f.setVisible(true);
		f.setExtendedState(f.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		f.setLayout(new FlowLayout(FlowLayout.RIGHT));

		f.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (!ignore) {
					System.out.println("a");
					if (comparison_mode == true && hover_index != -1
							&& (System.currentTimeMillis() - last_comparison_call) > 1000) {
						System.out.println(System.currentTimeMillis() - last_comparison_call);
						comparison_mode = false;
						preferred_index = hover_index;
						hover_index = -1;
						System.out.println("b");
					} else {
						comparison_mode = true;
						hover_index = -1;
						preferred_index = -1;
						last_comparison_call = System.currentTimeMillis();
						System.out.println("c");
					}
					System.out.println("d");
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

		});

		f.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_P) {
					pause = !pause;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		Timer paint_cycle = new Timer();
		paint_cycle.schedule(new TimerTask() {
			@Override
			public void run() {
				double x = MouseInfo.getPointerInfo().getLocation().getX(),
						y = MouseInfo.getPointerInfo().getLocation().getY();
				for (int i = 0; i < gi_list.size(); i++) {
					if (x > ((100 + (i * 300)) - 20) && x < ((100 + (i * 300)) + 260) && y > 610 && y < 940) {
						hover_index = i;
						break;
					}
					if (i == gi_list.size() - 1)
						hover_index = -1;
				}
			}
		}, 0, 100);
		color_wheel.add(Color.RED);
		color_wheel.add(Color.ORANGE);
		color_wheel.add(Color.YELLOW);
		color_wheel.add(Color.GREEN);
		color_wheel.add(Color.BLUE);
		color_wheel.add(Color.MAGENTA);
		color_wheel.add(Color.PINK);
		color_wheel.add(Color.BLACK);
	}

	public void begin() {
		for (GenInterface gi : gi_list) {
			new Thread(() -> {
				while (max == -1 || gi.current_generation < max) {
					if (!pause) {
						gi.procedeGenerations();
					}
					if (gi.current_generation == max
							&& gi.getPrecedenceViolationsProportion(gi.getFittestPopulationMember()) == 0.0
							&& gi.getExceedingIdleProportion(gi.getFittestPopulationMember()) == 0.0) {
						String type = "";
						if (gi.mutation_rate == .2) {
							type = "standard";
						} else {
							type = "novel";
						}
						try {
							writer.write("index: " + file_index + "/" + type + "/" + "runtime: "
									+ (System.currentTimeMillis() - gi.start_time) + "/" + "convergence_time: "
									+ (gi.convergence_time - gi.start_time) + "/" + "population_size: "
									+ gi.population.size() + "/" + "fitness: "
									+ gi.fitness(gi.getFittestPopulationMember(), false) + "/"
									+ "valid idle proportion: "
									+ gi.getValidIdleProportion(gi.getFittestPopulationMember()));
							writer.write(System.getProperty("line.separator"));
							active_lines--;
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if (gi.current_generation == max) {
						failed = true;
						active_lines--;
					}
				}

			}).start();
		}
	}

	public static void paint(Graphics g) {
		try {
			BufferedImage main_bi = new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D main_a = (Graphics2D) main_bi.getGraphics();
			BufferedImage bi = new BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB);
			Graphics2D a = (Graphics2D) bi.getGraphics();
			a.setBackground(Color.WHITE);
			a.fillRect(0, 0, 1000, 500);
			a.setColor(Color.GRAY);
			a.setStroke(new BasicStroke(3));
			a.drawLine(80, 80, 80, 420);
			a.drawLine(80, 420, 920, 420);
			if (!comparison_mode) {
				System.out.println("comparison_mode");
				GenInterface gi = gi_list.get(preferred_index);
				LinkedList<Double> y_positions = new LinkedList<>();
				y_positions.addAll(gi.best_fitnesses);
				y_positions.addAll(gi.worst_fitnesses);
				y_positions.addAll(gi.average_fitnesses);
				if (y_positions.size() > 1) {
					double delta_x = ((double) 840 / (gi.best_fitnesses.size() - 1));
					double y_max = Double.MIN_VALUE, y_min = Double.MAX_VALUE;
					for (int i = 0; i < y_positions.size(); i++) {
						if (y_positions.get(i) > y_max)
							y_max = y_positions.get(i);
						else if (y_positions.get(i) < y_min)
							y_min = y_positions.get(i);
					}
					a.setStroke(new BasicStroke(2));
					y_max = 1;
					y_min = (Math.floor(y_min * 10.0) / 10.0);

					int tick_distance = 0;
					if (gi.best_fitnesses.size() < 200) {
						tick_distance = 10;
					} else if (gi.best_fitnesses.size() < 2000) {
						tick_distance = 100;
					} else if (gi.best_fitnesses.size() < 20000) {
						tick_distance = 1000;
					}
					a.setColor(Color.GRAY);
					a.setFont(new Font("Calibri", Font.PLAIN, 12));
					double delta_tick = (double) 840 / gi.best_fitnesses.size();
					for (double i = 0; i <= (double) gi.best_fitnesses.size() / tick_distance; i++) {
						a.setColor(Color.GRAY);
						a.drawLine((int) (i * delta_tick * tick_distance) + 80, 410,
								(int) (i * delta_tick * tick_distance) + 80, 430);
						a.drawString("" + (int) (i * tick_distance), (int) (i * delta_tick * tick_distance) + 77, 440);
						a.setColor(new Color(240, 240, 240));
						if (i != 0)
							a.drawLine((int) (i * delta_tick * tick_distance) + 80, 80,
									(int) (i * delta_tick * tick_distance) + 80, 420);
					}
					double rounded_difference = y_max - y_min;
					delta_tick = 340.0 / (rounded_difference / .1);
					for (int i = 0; i <= (int) (rounded_difference / .1); i++) {
						a.setColor(Color.GRAY);
						a.drawLine(70, (int) (delta_tick * i) + 80, 90, (int) (delta_tick * i) + 80);
						a.drawString("" + (double) Math.round((y_max - (i * .1)) * 10) / 10, 50,
								(int) (delta_tick * i) + 84);
						a.setColor(new Color(240, 240, 240));
						if (i != (int) (rounded_difference / .1))
							a.drawLine(80, (int) (delta_tick * i) + 80, 920, (int) (delta_tick * i) + 80);
					}

					double range = y_max - y_min;
					a.setColor(Color.RED);
					for (int i = 0; i < gi.worst_fitnesses.size() - 1; i++) {
						a.drawLine((int) (delta_x * i) + 80,
								(340 - (int) (340.0 * ((gi.worst_fitnesses.get(i) - y_min) / range))) + 80,
								(int) (delta_x * (i + 1)) + 80,
								(340 - (int) (340.0 * ((gi.worst_fitnesses.get(i + 1) - y_min) / range))) + 80);
					}
					a.setColor(Color.BLACK);
					for (int i = 0; i < gi.average_fitnesses.size() - 1; i++) {
						a.drawLine((int) (delta_x * i) + 80,
								(340 - (int) (340.0 * ((gi.average_fitnesses.get(i) - y_min) / range))) + 80,
								(int) (delta_x * (i + 1)) + 80,
								(340 - (int) (340.0 * ((gi.average_fitnesses.get(i + 1) - y_min) / range))) + 80);
					}
					a.setColor(Color.GREEN);
					for (int i = 0; i < gi.best_fitnesses.size() - 1; i++) {
						a.drawLine((int) (delta_x * i) + 80,
								(340 - (int) (340.0 * ((gi.best_fitnesses.get(i) - y_min) / range))) + 80,
								(int) (delta_x * (i + 1)) + 80,
								(340 - (int) (340.0 * ((gi.best_fitnesses.get(i + 1) - y_min) / range))) + 80);
					}
					a.setColor(Color.GRAY);
					a.setFont(new Font("Calibri", Font.PLAIN, 20));
					a.drawString("Generation", 470, 470);
					a.rotate(-Math.PI / 2.0);
					a.drawString("Fitness", -300, 30);
				}
				main_a.setColor(new Color(230, 230, 230));
				main_a.fillRect(0, 0, f.getWidth(), f.getHeight());
				main_a.setFont(new Font("Calibri", Font.PLAIN, 20));
				main_a.setColor(Color.BLACK);
				main_a.drawString("best_fitness: "
						+ (double) Math.round(gi.best_fitnesses.get(gi.best_fitnesses.size() - 1) * 100000d) / 100000d,
						100, 600);
				main_a.drawString("prec_viol_prop: " + (double) Math
						.round((gi.getPrecedenceViolationsProportion(gi.getFittestPopulationMember())) * 100000d)
						/ 100000d, 100, 620);
				main_a.drawString("viol_idle_prop: " + (double) Math
						.round((gi.getExceedingIdleProportion(gi.getFittestPopulationMember())) * 100000d) / 100000d,
						100, 640);
				main_a.drawString("valid_idle_prop: "
						+ (double) Math.round((gi.getValidIdleProportion(gi.getFittestPopulationMember())) * 100000d)
								/ 100000d,
						100, 660);
				main_a.drawString("stations_count: " + gi.getFittestPopulationMember().stations.size(), 100, 680);
				main_a.drawString("worst_fitness: "
						+ (double) Math.round(gi.worst_fitnesses.get(gi.worst_fitnesses.size() - 1) * 100000d)
								/ 100000d,
						370, 600);
				main_a.drawString("prec_viol_prop: " + (double) Math
						.round((gi.getPrecedenceViolationsProportion(gi.getLeastFittestPopulationMember())) * 100000d)
						/ 100000d, 370, 620);
				main_a.drawString("viol_idle_prop: " + (double) Math
						.round((gi.getExceedingIdleProportion(gi.getLeastFittestPopulationMember())) * 100000d)
						/ 100000d, 370, 640);
				main_a.drawString("valid_idle_prop: "
						+ (double) Math.round((gi.getValidIdleProportion(gi.getFittestPopulationMember())) * 100000d)
								/ 100000d,
						370, 660);
				main_a.drawString("stations_count: " + gi.getLeastFittestPopulationMember().stations.size(), 370, 680);
				main_a.drawString(
						"average_fitness: " + (double) Math
								.round(gi.average_fitnesses.get(gi.average_fitnesses.size() - 1) * 100000d) / 100000d,
						660, 600);
				main_a.drawString("current_generation: " + gi.current_generation, 910, 600);
				main_a.drawString("file_name: " + gi.filename, 100, 760);
				main_a.drawString("mutation_rate: " + gi.mutation_rate, 100, 800);
				main_a.drawString("population_size: " + gi.population.size(), 100, 840);
				main_a.drawString("order_strength: " + gi.order_strength, 100, 880);
				main_a.drawString("optimal_stations_count: " + gi.optimal_stations_count, 100, 920);
				main_a.drawString("total_task_time: " + gi.total_task_time, 100, 960);
				main_a.drawString("cycle_time: " + gi.cycle_time, 100, 1000);
				main_a.setColor(Color.GREEN);
				main_a.fillRect(90, 580, 7, 70);
				main_a.setColor(Color.RED);
				main_a.fillRect(360, 580, 7, 70);
				main_a.setColor(Color.GRAY);
				main_a.fillRect(650, 580, 7, 70);
				main_a.drawImage(bi, 50, 50, null);
				g.drawImage(main_bi, 0, 0, null);
			} else {
				GenInterface largest = null;
				int max = Integer.MIN_VALUE;
				for (GenInterface gi : gi_list) {
					if (gi.average_fitnesses.size() > max) {
						max = gi.average_fitnesses.size();
						largest = gi;
					}
				}

				LinkedList<Double> y_positions = new LinkedList<>();
				for (GenInterface gi : gi_list) {
					y_positions.addAll(gi.average_fitnesses);
				}
				boolean permit = true;
				for (GenInterface gi : gi_list)
					if (gi.average_fitnesses.size() == 0)
						permit = false;
				if (permit) {
					double delta_x = ((double) 840 / (largest.best_fitnesses.size() - 1));
					double y_max = -1000, y_min = 1000;
					for (int i = 0; i < y_positions.size(); i++) {
						if (y_positions.get(i) > y_max)
							y_max = y_positions.get(i);
						if (y_positions.get(i) < y_min)
							y_min = y_positions.get(i);
					}
					a.setStroke(new BasicStroke(2));
					y_max = 1;
					y_min = (Math.floor(y_min * 10.0) / 10.0);

					int tick_distance = 0;
					if (largest.best_fitnesses.size() < 200) {
						tick_distance = 10;
					} else if (largest.best_fitnesses.size() < 2000) {
						tick_distance = 100;
					} else if (largest.best_fitnesses.size() < 20000) {
						tick_distance = 1000;
					}
					a.setFont(new Font("Calibri", Font.PLAIN, 12));
					double delta_tick = (double) 840 / largest.best_fitnesses.size();
					for (double i = 0; i <= (double) largest.best_fitnesses.size() / tick_distance; i++) {
						a.setColor(Color.GRAY);
						a.drawLine((int) (i * delta_tick * tick_distance) + 80, 410,
								(int) (i * delta_tick * tick_distance) + 80, 430);
						a.drawString("" + (int) (i * tick_distance), (int) (i * delta_tick * tick_distance) + 77, 440);
						a.setColor(new Color(240, 240, 240));
						if (i != 0)
							a.drawLine((int) (i * delta_tick * tick_distance) + 80, 80,
									(int) (i * delta_tick * tick_distance) + 80, 420);
					}
					double rounded_difference = 1 - y_min;
					delta_tick = 340.0 / (rounded_difference / .1);
					for (int i = 0; i <= (int) (rounded_difference / .1); i++) {
						a.setColor(Color.GRAY);
						a.drawLine(70, (int) (delta_tick * i) + 80, 90, (int) (delta_tick * i) + 80);
						a.drawString("" + (double) Math.round((y_max - (i * .1)) * 10) / 10, 50,
								(int) (delta_tick * i) + 84);
						a.setColor(new Color(240, 240, 240));
						if (i != (int) (rounded_difference / .1) + 1)
							a.drawLine(80, (int) (delta_tick * i) + 80, 920, (int) (delta_tick * i) + 80);
					}

					double range = 1 - y_min;
					for (int x = 0; x < gi_list.size(); x++) {
						GenInterface gi = gi_list.get(x);
						a.setColor(color_wheel.get(x % color_wheel.size()));
						for (int i = 0; i < gi.average_fitnesses.size() - 1; i++) {
							a.drawLine((int) (delta_x * i) + 80,
									(340 - (int) (340.0 * ((gi.average_fitnesses.get(i) - y_min) / range))) + 80,
									(int) (delta_x * (i + 1)) + 80,
									(340 - (int) (340.0 * ((gi.average_fitnesses.get(i + 1) - y_min) / range))) + 80);
						}
					}
					a.setColor(Color.GRAY);
					a.setFont(new Font("Calibri", Font.PLAIN, 20));
					a.drawString("Generation", 470, 470);
					a.rotate(-Math.PI / 2.0);
					a.drawString("Fitness", -300, 30);

					main_a.setColor(new Color(230, 230, 230));
					main_a.fillRect(0, 0, f.getWidth(), f.getHeight());
					main_a.setFont(new Font("Calibri", Font.PLAIN, 20));
					main_a.setColor(Color.BLACK);
					int i = 0;
					main_a.drawString("file_name: " + gi_list.get(0).filename, 100, 600);
					if (hover_index != -1)
						main_a.drawRect((100 + (hover_index * 300)) - 20, 610, 260, 330);
					for (GenInterface gi : gi_list) {
						main_a.setColor(Color.BLACK);
						main_a.drawString("average_fitness: " + (double) Math
								.round(gi.average_fitnesses.get(gi.average_fitnesses.size() - 1) * 100000d) / 100000d,
								100 + (i * 300), 640);
						main_a.drawString("mutation_rate: " + gi.mutation_rate, 100 + (i * 300), 680);
						main_a.drawString("population_size: " + gi.population.size(), 100 + (i * 300), 720);
						main_a.drawString("order_strength: " + gi.order_strength, 100 + (i * 300), 760);
						main_a.drawString("best_stations_count: " + gi.getFittestPopulationMember().stations.size(),
								100 + (i * 300), 800);
						main_a.drawString("optimal_stations_count: " + gi.optimal_stations_count, 100 + (i * 300), 840);
						main_a.drawString("total_task_time: " + gi.total_task_time, 100 + (i * 300), 880);
						main_a.drawString("cycle_time: " + gi.cycle_time, 100 + (i * 300), 920);
						main_a.setColor(color_wheel.get(i % color_wheel.size()));
						main_a.fillRect(100 + (i * 300) - 10, 620, 7, 310);
						i++;
					}
				}
				main_a.drawImage(bi, 50, 50, null);
				g.drawImage(main_bi, 0, 0, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) throws IOException {
		/*
		 * System.out.
		 * println("new, data_size, index, population_size, mutation_rate, hybridization"
		 * ); Scanner sc = new Scanner(System.in); LinkedList<GenInterface> temp_gi_list
		 * = new LinkedList<>(); boolean cont = true; while (cont) { String nextline =
		 * sc.nextLine(); if (nextline.endsWith("runtime")) { temp_gi_list.add(new
		 * GenInterface("SALBP-1_data_set/small data set_n=20/instance_n=20_" +
		 * nextline.substring(0, nextline.indexOf(" ")) + ".alb", 100, 1, true));
		 * temp_gi_list.add(new
		 * GenInterface("SALBP-1_data_set/small data set_n=20/instance_n=20_" +
		 * nextline.substring(0, nextline.indexOf(" ")) + ".alb", 100, .2, false));
		 * break; } if (nextline.equals("begin")) { break; } else if
		 * (nextline.contains("end")) { System.exit(0); } else if
		 * (nextline.contains("new")) { String[] comps = nextline.split(" "); boolean
		 * hybrid = false; if (comps[5].equals("true")) { hybrid = true; } if
		 * (comps[1].equals("small")) { temp_gi_list.add(new
		 * GenInterface("SALBP-1_data_set/small data set_n=20/instance_n=20_" + comps[2]
		 * + ".alb", Integer.parseInt(comps[3]), Double.parseDouble(comps[4]), hybrid));
		 * } else if (comps[1].equals("medium")) { temp_gi_list.add(new
		 * GenInterface("SALBP-1_data_set/medium data set_n=50/instance_n=50_" +
		 * comps[2] + ".alb", Integer.parseInt(comps[3]), Double.parseDouble(comps[4]),
		 * hybrid)); } else if (comps[1].equals("large")) { temp_gi_list.add(new
		 * GenInterface("SALBP-1_data_set/large data set_n=100/instance_n=100_" +
		 * comps[2] + ".alb", Integer.parseInt(comps[3]), Double.parseDouble(comps[4]),
		 * hybrid)); } else if (comps[1].equals("very large")) { temp_gi_list.add(new
		 * GenInterface("SALBP-1_	data_set/very large data set_n=1000/instance_n=1000_"
		 * + comps[2] + ".alb", Integer.parseInt(comps[3]),
		 * Double.parseDouble(comps[4]), hybrid)); } } } UserInterface UI = new
		 * UserInterface(true); UI.gi_list.addAll(temp_gi_list); sc.close();
		 */

		int pop_size = 50;
		int n = 0;

		UserInterface UI;
		for (int i = 1; i < 100; i++) {
			System.out.println("");
			UI = new UserInterface(true, 150, i + 1);
			UI.gi_list.add(new GenInterface("SALBP-1_data_set/medium data set_n=50/instance_n=50_" + (i + 1) + ".alb",
					pop_size, 1, true));
			UI.gi_list.add(new GenInterface("SALBP-1_data_set/medium data set_n=50/instance_n=50_" + (i + 1) + ".alb",
					pop_size, .2, false));
			UI.active_lines = 2;
			UI.begin();
			while (UI.active_lines != 0) {
				System.out.print("");
				for (GenInterface gi : UI.gi_list) {
					// System.out.println(gi.null_clock);
					if (gi.null_clock > 1000) {
						System.out.println("failed");
						UI.failed = true;
						UI.active_lines = 0;
					}
				}
			}
			if (UI.failed && pop_size < 1000) {
				pop_size += 100;
				i--;
				UI.gi_list.clear();
				UI.writer.write(System.getProperty("line.separator"));
				UI.writer.close();
				continue;
			}
			pop_size = 50;
			UI.gi_list.clear();
			UI.writer.write(System.getProperty("line.separator"));
			UI.writer.close();
			UI.comparison_mode = false;
			UI.hover_index = -1;
			UI.preferred_index = -1;
		}
		System.exit(0);
	}
}
