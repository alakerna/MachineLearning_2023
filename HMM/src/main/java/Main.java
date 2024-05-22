
import java.util.HashMap;

import data.DataLoader;
import model.EnhancedHMM;
import model.HMM;
import util.Util;

public class Main {

	public Main() {
	}

	public static void main(String[] args) {
		String trainDataPath = "data/trivia10k13train.bio";
		String testDataPath = "data/trivia10k13test.bio";
		
		final long startTime = System.currentTimeMillis();
		
		DataLoader trainLoader = new DataLoader(trainDataPath);
		
		// // DEBUG: test 
		// System.out.println(trainLoader.getDoc(0));
		// System.out.println(trainLoader.getStates(0));

		// HMM model = new HMM();
		EnhancedHMM model = new EnhancedHMM();
		model.fit(trainLoader);
		
		DataLoader testLoader = new DataLoader(testDataPath, false);
		HashMap<Integer, Integer[]> decodedPath = model.decodeAllDocs(testLoader);
		
		// Compute the performance metrics
		Util.computePerformance(decodedPath, testLoader, null);
		
		// Report the runtime
		final long endTime = System.currentTimeMillis();
		System.out.println("\nTotal execution time: " + (endTime - startTime)/1000.0);
	}
}
