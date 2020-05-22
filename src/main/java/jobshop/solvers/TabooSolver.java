package jobshop.solvers;

import java.util.List;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.DescentSolver.Swap;

public class TabooSolver implements Solver {

	int maxIter;
	int dureeTaboo;
	
	public TabooSolver(int maxIter, int dureeTaboo) {
		this.maxIter = maxIter;
		this.dureeTaboo = dureeTaboo;
	}
	
	public Result solve(Instance instance, long deadline) {
		
		   long start = System.currentTimeMillis();
	       Schedule initsol = new GloutSolver(GloutSolver.Priority.EST_LRPT).solve(instance, deadline).schedule;
	       int min = initsol.makespan();
	       int currentSpan = min;
	       int niter = 0;
	       ResourceOrder minSol = ResourceOrder.fromSchedule(initsol);
	       ResourceOrder currentSol = ResourceOrder.fromSchedule(initsol);
           int[][][][] taboos = new int[instance.numJobs][instance.numTasks][instance.numJobs][instance.numTasks]; 
	       while(niter < this.maxIter) {
	    	   if(System.currentTimeMillis() - start >= deadline) {
	    		   return new Result(minSol.instance, minSol.toSchedule(), Result.ExitCause.Timeout);
	    	   }
	    	   Swap bestSwap = null;
	    	   niter++;
	    	   List<DescentSolver.Block> blocks = DescentSolver.blocksOfCriticalPath(currentSol);
	    	   ResourceOrder saveCurrentSol = new ResourceOrder(currentSol.instance, currentSol.order);
	    	   ResourceOrder minNeighbor = null;
	    	   int minN = -1;
	    	   for(DescentSolver.Block block : blocks) {
	    		   List<DescentSolver.Swap> swaps = DescentSolver.neighbors(block);
	    		   for(DescentSolver.Swap swap : swaps) {
	    			   Task t1 = saveCurrentSol.order[swap.machine][swap.t1];
	    			   Task t2 = saveCurrentSol.order[swap.machine][swap.t2];
	    			   if(taboos[t1.job][t1.task][t2.job][t2.task] == 0 || niter >= taboos[t1.job][t1.task][t2.job][t2.task]) {
	    			    currentSol = new ResourceOrder(saveCurrentSol.instance, saveCurrentSol.order);
	    			    swap.applyOn(currentSol);
	    			    currentSpan = currentSol.toSchedule().makespan();
	    			    if(minN == -1 || currentSpan < minN) {
	    			    	minN = currentSpan;
	    			    	minNeighbor = currentSol;
	    			    	bestSwap = swap;
	    			    }
	    			   }
	    		   }
	    	   }
	    	   if(bestSwap != null) {
			   Task t1 = saveCurrentSol.order[bestSwap.machine][bestSwap.t1];
			   Task t2 = saveCurrentSol.order[bestSwap.machine][bestSwap.t2];
			   taboos[t2.job][t2.task][t1.job][t1.task] = niter+this.dureeTaboo;					   
	    	   currentSol = minNeighbor;
	    	   if(minN < min) {
	    		   min = minN;
	    		   minSol = minNeighbor;
	    	   }
	    	   }
	       }
	       return new Result(minSol.instance, minSol.toSchedule(), Result.ExitCause.Blocked);
		
	}
}