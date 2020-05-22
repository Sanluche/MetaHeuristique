package jobshop.encodings;

import java.util.Arrays;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

public class ResourceOrder extends Encoding{

	public final Instance pb;

	public final Task[][] order;

	public ResourceOrder(Instance pb, Task[][] orders) {
		super(pb);
		this.pb = pb;
		this.order = new Task[pb.numMachines][];
		for(int j = 0 ; j < pb.numMachines ; j++) {
			this.order[j] = Arrays.copyOf(orders[j], pb.numJobs);
		}
	}
	public ResourceOrder(Instance pb) {
		super(pb);
		this.pb = pb;
		this.order = new Task[pb.numMachines][pb.numJobs];
	}
	public Schedule toSchedule() {
		int times[][] = new int[this.pb.numJobs][this.pb.numTasks];
		int nextTask[] = new int[this.pb.numJobs];
		int nextFreeTimeResource[] = new int[this.pb.numMachines];
		int NumberWorkDoneMachine[] = new int[this.pb.numMachines];
		boolean continuer = true;
				while(continuer) {
					continuer = false;
					for(int job = 0; job < this.pb.numJobs; job++) {
						int task = nextTask[job];
						if (task < this.pb.numTasks) {
							Task currentTask = new Task(job, task);
							int need = this.pb.machine(job, task);
							Task currentTaskMachine = this.order[need][NumberWorkDoneMachine[need]];
							if(currentTask.equals(currentTaskMachine)) {
								int est = task == 0 ? 0 : times[job][task-1] + instance.duration(job, task-1);
								est = Math.max(est, nextFreeTimeResource[need]);
								times[job][task] = est;
								nextTask[job]++;
								NumberWorkDoneMachine[need]++;
								nextFreeTimeResource[need] = times[job][task] + instance.duration(job,  task);
							}
							continuer |= nextTask[job] < this.pb.numTasks; 
						}

					}

				}
				return new Schedule(this.pb, times);


	}
	
    static int nextAction(int[] nextTache, Schedule sc) {
    	int min = -1;
    	int indexMin = -1;
    	for(int job = 0; job < sc.pb.numJobs; job++) {
    		if(nextTache[job] < sc.pb.numTasks) {
	    		int currentValue = sc.startTime(job, nextTache[job]);
	    		if(currentValue < min || min == -1) {
	    			min = currentValue;
	    			indexMin = job;
	    		}
    		}
    	}
    	return indexMin;
    }
    
    public static ResourceOrder fromSchedule(Schedule sc) {
    	int[] nextTache = new int[sc.pb.numJobs];
    	int[] numberWorkDoneMachine = new int[sc.pb.numMachines];
    	
    	ResourceOrder ro = new ResourceOrder(sc.pb);
    	for(int i = 0; i < sc.pb.numJobs * sc.pb.numMachines; i++) {
    		int nextJob = nextAction(nextTache, sc);
    		if(nextJob != -1) {
	    		int nextTask = nextTache[nextJob];
	    		int machine = sc.pb.machine(nextJob, nextTask);
	    		ro.order[machine][numberWorkDoneMachine[machine]] = new Task(nextJob, nextTask);
	    		numberWorkDoneMachine[machine]++;
	    		nextTache[nextJob]++;
    		}
    	}
    	return ro;
    }
    
    public String toString() {
    	String resultat = "";
    	for(int machine = 0; machine < this.pb.numMachines; machine++) {
    		resultat += "r"+machine+" : "+this.order[machine][0];
    		for(int job = 1; job < this.pb.numJobs; job++) {
    			
    			resultat += ", "+this.order[machine][job].toString();
    		}
    		resultat += "\n";
    	}
    	return resultat;
    }


		
	}
