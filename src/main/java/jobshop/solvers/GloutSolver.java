package jobshop.solvers;

import java.util.ArrayList;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class GloutSolver implements Solver{

	
	public enum Priority  {
			SPT, LRPT, EST_SPT, EST_LRPT
	}
	private Priority prio;
	
	private class EarliestTasks {
		public ArrayList<Task> earliest;
		public int time;
		
		public EarliestTasks(ArrayList<Task> earliest, int time) {
			this.earliest = earliest;
			this.time = time;
		}
	}
	
	public GloutSolver(Priority prio) {
		this.prio = prio;
	}
	private Task selectTask(ArrayList<Task> doable, Instance instance) {
		
		switch(this.prio) {
		case SPT:
		case EST_SPT:
		int min = -1;
		Task shortestTask = new Task(-1, -1);
		for(Task task : doable) {
			int currentDuration = instance.duration(task.job, task.task);
			if(currentDuration < min || min == -1) {
				min = currentDuration;
				shortestTask = task;
			}
		}
		return shortestTask;
		case LRPT:
		default:
			int max = -1;
			Task longestJob = new Task(-1, -1);
			for(Task task : doable) {
				int currentDuration = instance.duration(task.job, task.task);
				for(int nTask = task.task + 1; nTask < instance.numTasks; nTask++) {
					currentDuration += instance.duration(task.job, nTask);
				}
				if(currentDuration > max || max == -1) {
					max = currentDuration;
					longestJob = task;
				}
			}
			return longestJob;
			
		}
		
			
	}
	
	private EarliestTasks earliestTasks(ArrayList<Task> doable, int[][] times, int[] nextFreeTimeResource, Instance instance) {
		ArrayList<Task> earliest = new ArrayList<Task>();
		int min = -1;
		for(Task task : doable) {
			int machine = instance.machine(task.job, task.task);
			int est = task.task == 0 ? 0 : times[task.job][task.task - 1]+instance.duration(task.job, task.task-1);
			est = Math.max(est, nextFreeTimeResource[machine]);
			if(est < min || min == -1) {
				min = est;
				earliest.clear();
				earliest.add(task);
			}
			else if(est == min)
				earliest.add(task);
		}
		return new EarliestTasks(earliest, min);
		
	}
	public Result solve(Instance instance, long deadline){
		ArrayList<Task> doable = new ArrayList<Task>();
		switch(this.prio) {
		case SPT:
		case LRPT: 
		ResourceOrder sol = new ResourceOrder(instance);
		int[] numberWorkDoneMachine = new int[instance.numMachines];
		for(int job = 0; job < instance.numJobs; job++) {
			doable.add(new Task(job, 0));
		}
		while(!doable.isEmpty()) {
			Task current = selectTask(doable, instance);
			doable.remove(current);
			int machine = instance.machine(current.job, current.task);
			sol.order[machine][numberWorkDoneMachine[machine]] = current;
			numberWorkDoneMachine[machine]++;
			if(current.task < instance.numTasks - 1) {
				doable.add(new Task(current.job, current.task + 1));
			}
		}
		
		
		return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
		
		case EST_SPT:
		default:
			int[][] times = new int[instance.numJobs][instance.numMachines];
			int[] nextFreeTimeResource = new int[instance.numMachines];
			EarliestTasks earliest = new EarliestTasks(new ArrayList<Task>(), 0);
			for(int job = 0; job < instance.numJobs; job++) {
				doable.add(new Task(job, 0));
				earliest.earliest.add(new Task(job, 0));
			}
			while(!doable.isEmpty()) {
			Task currentTask = selectTask(earliest.earliest, instance);
			doable.remove(currentTask);
			int est = earliest.time;
			times[currentTask.job][currentTask.task] = est;
			nextFreeTimeResource[instance.machine(currentTask.job, currentTask.task)] = est + instance.duration(currentTask.job, currentTask.task);
			if(currentTask.task < instance.numTasks - 1)
				doable.add(new Task(currentTask.job, currentTask.task + 1));
			earliest = earliestTasks(doable, times, nextFreeTimeResource, instance);
			}
			return new Result(instance, new Schedule(instance, times), Result.ExitCause.Blocked);
				
		}
	}
}
