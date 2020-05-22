package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
        
        public String toString() {
        	return this.machine+" "+this.firstTask+"-"+this.lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        final int machine;
        final int t1;
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
        	Task aux = order.order[this.machine][t1];
        	order.order[this.machine][this.t1] = order.order[this.machine][this.t2];
        	order.order[this.machine][this.t2] = aux;    			
        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
       boolean ended = false;
       long start = System.currentTimeMillis();
       Schedule initsol = new GloutSolver(GloutSolver.Priority.EST_LRPT).solve(instance, deadline).schedule;
       int min = initsol.makespan();
       int currentSpan = min;
       ResourceOrder minSol = ResourceOrder.fromSchedule(initsol);
        while(!ended) {
        	if(System.currentTimeMillis() - start >= deadline) {
	    		   return new Result(minSol.instance, minSol.toSchedule(), Result.ExitCause.Timeout);
	    	   }
        	ended = true;
        	List<Block> blocks = blocksOfCriticalPath(minSol);
			ResourceOrder saveCurrentSol = new ResourceOrder(minSol.instance, minSol.order);
        	for(Block block : blocks) {
        		List<Swap> swaps = neighbors(block);
        		for(Swap swap : swaps) {
        			ResourceOrder currentSol = new ResourceOrder(saveCurrentSol.instance, saveCurrentSol.order);
        			swap.applyOn(currentSol);
        			currentSpan = currentSol.toSchedule().makespan();
        			if(currentSpan < min) {
        				ended = false;
        				minSol = currentSol;
        				min = currentSpan;
        			}
        		}
        	}	
        }
        return new Result(minSol.instance, minSol.toSchedule(), Result.ExitCause.Blocked);
    }

    private static int indexOf(ResourceOrder ro, int machine, Task task) {
    	for(int j = 0; j < ro.instance.numJobs; j++) {
    		if(ro.order[machine][j].equals(task))
    			return j;
    	}
    	return -1;
    }
    /** Returns a list of all blocks of the critical path. */
    public static List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        LinkedList<Block> result = new LinkedList<Block>();
        int currentMachine = -1;
        int currentFirstTask = -1;
        int nbConsecTasks = 1;

        for(Task task : criticalPath) {
        	if(currentMachine == -1) {
        	currentMachine = order.pb.machine(task.job, task.task);
        	currentFirstTask = indexOf(order, currentMachine, task);
        	}
        	else if(currentMachine != order.pb.machine(task.job, task.task)) {
        		if(nbConsecTasks > 1) {
        		result.add(new Block(currentMachine, currentFirstTask, currentFirstTask+nbConsecTasks-1));
        		}
        		currentMachine = order.pb.machine(task.job, task.task);
        		currentFirstTask = indexOf(order, currentMachine, task);
        		nbConsecTasks = 1;
        	}
        	else {
        		nbConsecTasks++;
        	}
        }
        if(nbConsecTasks > 1) {
        	result.add(new Block(currentMachine, currentFirstTask, currentFirstTask+nbConsecTasks-1));
        }
        return result;
    }
 
    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    public static List<Swap> neighbors(Block block) {
        ArrayList<Swap>result = new ArrayList<Swap>();
        if(block.lastTask - block.firstTask == 1) {
        	result.add(new Swap(block.machine, block.firstTask, block.lastTask));
        }
        else {
        	result.add(new Swap(block.machine, block.firstTask, block.firstTask+1));
        	result.add(new Swap(block.machine, block.lastTask - 1, block.lastTask));
        }
        return result;
    }

}
