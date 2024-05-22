package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ListIterator;

import data.DataLoader;

public class EnhancedHMM extends Model {
	
	/**
	 * TODO: Define any necessary variables here 
	 */
	
	public EnhancedHMM() {
		super();
		double alpha = 0.8;
		double threshold = 1e-6;
		double logEpsilon = -20.723266;
	}
	
	/**
	 * TODO: Complete the fit method
	 * Tip: probabilities are computed based on visit counts
	 * 		Update and use _stateVisitCounts, _state2StateVisitCounts, _obsVisitCounts arrays
	 */
	@Override
	public void fit(DataLoader trainLoader) {
				
		// Initialize the sizes of the visit count arrays and probability tables 
		initialize();
		
		//////////////////////////////////////////////////////////////
		/**
		 * TODO: Update the visit count arrays
		 */ //
		//////////////////////////////////////////////////////////////

		for(int k=0; k<trainLoader._numDocs; k++){  // iterates over num of docs
			_stateVisitCounts[0] += 1; //add start state at the start of every doc string
			ArrayList<State> stateDoc = trainLoader.getStates(k); //get states of  doc (k)
			ListIterator<State> it = stateDoc.listIterator();
			ListIterator<State> it2 = stateDoc.listIterator();

			//needs to happen inside while loop
			State next1, next2;
			next2 = it2.next();
			_state2StateVisitCounts[0][stateDoc.get(0).getID()] +=1; //add 1, start to first obs
			while(it2.hasNext()){
				next1 = it.next();
				next2 = it2.next();
				for(State row : State.values()){
					if(row == next1){ // if table i'm creating (row) matches the state token
						Integer rowId = row.getID();
						_stateVisitCounts[rowId] += 1;
						for(State col : State.values()){ // if dest col in table matches next state token
							if(col == next2){
								Integer colId = col.getID();
								// // DEBUG: print your state2state visits
								// System.out.println("row " + row + " col "+col);	
								_state2StateVisitCounts[rowId][colId] +=1;
							}
						}
					}
				}		

			}
			next1 = it.next();
			_stateVisitCounts[_sizeStateSpace-1] += 1; //add end state at the end of every doc string
			_state2StateVisitCounts[next1.getID()][26] +=1;	
		}
		
		// DEBUG: print your solution for state visit counts
		// for(int i=0; i<_sizeStateSpace; i++){
		// 	System.out.println(_stateVisitCounts[i]);
		// }
		
		for(int doc=0; doc< trainLoader._numDocs; doc++){
			ArrayList<String> wordDoc = trainLoader.getDoc(doc);
			ListIterator<String> wordIt= wordDoc.listIterator();

			ArrayList<State> stateDoc = trainLoader.getStates(doc);
			ListIterator<State> stateIt = stateDoc.listIterator();

			while(wordIt.hasNext() && stateIt.hasNext()){
				String next = wordIt.next();
				State next2 = stateIt.next();
				Integer wordId = DataLoader.getIndexOfWord(next); //defined in a static manner because it is related to the class universally, not based on object instantiation
				Integer stateId = next2.getID();
				_obsVisitCounts[stateId][wordId] += 1;
			}
		}



		// //////////////////////////////////////////////////////////////
		/** 
		 * TODO: Fit the transition CPT
		 * Tip: use _transitionProbability.setValue to set the value of the CPT
		 */

		for (State srcState : State.values()) {
			Integer srcId = srcState.getID();
			double denom = _stateVisitCounts[srcId] + _sizeStateSpace;
			for (State destState : State.values()) {
				
				Integer destId = destState.getID();
				double num = _state2StateVisitCounts[srcId][destId] + 0.87;
				double value = num/denom;

				if( Math.abs(num) < 1e-6 ){
					if((destId != 0 && srcId != 26 ) || !(srcId == 0 && destId == 26)){ 
						value = EPSILON;
					}
				}
				if((srcId == 26 || destId == 0 ) || (srcId == 0 && destId == 26)){
					value = 0.0;
				}
				//double value = num/denom;
				_transitionProbability.setValue(srcId, destId, value);
			}
		}
		//////////////////////////////////////////////////////////////
		
		//////////////////////////////////////////////////////////////
		/**
		 * 
		 * TODO: Fit the emission CPT
		 * Tip: use _emissionProbability.setValue to set the value of the CPT
		 */
		//////////////////////////////////////////////////////////////
	
		for(State s : State.values()) {
			Integer sId = s.getID();
			double denom = _stateVisitCounts[sId] + DataLoader.numWords;
			for(int wId=0; wId< DataLoader.numWords ; wId++){
				double num = _obsVisitCounts[sId][wId] + 0.87; //alpha=0.87 laplace smoothing
				double value = (num+0.8)/denom;
				 
				if(Math.abs(value) < 1e-6){
					value = EPSILON;
				}
				if(sId == 0 || sId == 26){ //start or end state
					value = 0.0;
				}
				_emissionProbability.setValue(sId, wId, value);
			}
		}


		// Re-normalize the probabilities to account for numerical errors
		_emissionProbability.normalize();
		_transitionProbability.normalize();
	}

	/**
	 * 	Decodes the latent states (i.e. finds out the most likely path of the states) 
	 * given a single document (i.e. a sequence of observations) via the Viterbi algorithm.
	 * 
	 * @param doc	The list of word tokens of a single document
	 * @return		An array of state IDs representing the most likely path of state progression
	 */
		//////////////////////////////////////////////////////////////
		/**
		 * 
		 * TODO: Decode the sequence of the most likely state IDs given the list of word tokens
		 * 
		 * Note: the length of the returned sequence should be 'T + 2' if T is the total number 
		 * of word tokens in doc.
		 * 
		 */
		//////////////////////////////////////////////////////////////

	@Override
	public Integer[] decode(ArrayList<String> doc) {
		//Integer[] mostLikelyPath = null; 
		Integer docSize = doc.size();
		Integer endStateID = State.END.getID();
		Integer[] mostLikelyPath = new Integer[docSize+2];  // maybe add +2 later check with jina

		Double[][] trellis = new Double[State.SIZE][docSize+1]; // +2 to incldue start and end date per doc
		Integer[][] psi = new Integer[State.SIZE][docSize+1];

		
		// code snippet to deal with first word probabilities
	
		int firstTokenID = DataLoader.getIndexOfWord(doc.get(0));
		
		// for (State state : State.values()){
		for (int stateID = 0; stateID < State.SIZE;  stateID ++){
			double intialPi = _transitionProbability.getLogProbability(0,stateID);
			double emissProb;
			// if this is a completely new word, never seen
			if(firstTokenID == -1){
				emissProb = -20.723266;
			}else{
				emissProb = _emissionProbability.getLogProbability(stateID,firstTokenID);
			}
			double firstDelta_tm1 = intialPi + emissProb;	
			trellis[stateID][0] = firstDelta_tm1;
			psi[stateID][0] = 0; //most probable path at t=0 for firts word is start state.
		}

		
		// traverse words in the document (each word is a time step)
		for (int t=1; t <docSize; t ++){

			int tokenID = DataLoader.getIndexOfWord(doc.get(t));

			// for(State currentState : State.values()){ // where you possibly could be right now
			for (int currentStateIdx = 0; currentStateIdx < State.SIZE;  currentStateIdx ++){
				Double maxDelta_t = Double.NEGATIVE_INFINITY;
				Double delta_tm1;
				Integer maxID = null;
				double emissProb;
				if(tokenID == -1){
					emissProb = -20.723266;
				}else{
					emissProb = _emissionProbability.getLogProbability(currentStateIdx,tokenID);
				}
				for (int sourceStateIdx = 0; sourceStateIdx < State.SIZE;  sourceStateIdx ++){ // where you came from
					double transProb = _transitionProbability.getLogProbability(sourceStateIdx,currentStateIdx);
					double prob = trellis[sourceStateIdx][t-1];
					delta_tm1 = prob+transProb+emissProb;
					if(delta_tm1 >= maxDelta_t){ 
						maxDelta_t = delta_tm1;
						maxID = sourceStateIdx;
					}	
				}
				trellis[currentStateIdx][t] = maxDelta_t;
				psi[currentStateIdx][t] = maxID;
			}
		}


		Arrays.fill(trellis[endStateID], Double.NEGATIVE_INFINITY); 

		Integer lastMaxID = null;
		Double maxLastDelta_t = Double.NEGATIVE_INFINITY;

		for (int sourceStateID = 0; sourceStateID < State.SIZE;  sourceStateID ++){

			Double lastDelta_tm1 = trellis[sourceStateID][docSize-1] + _transitionProbability.getLogProbability(sourceStateID,endStateID);
			if (lastDelta_tm1 >= maxLastDelta_t){
				maxLastDelta_t = lastDelta_tm1;
				lastMaxID = sourceStateID;
			}
		}
		trellis[endStateID][docSize] = maxLastDelta_t;

		
		for (int stateID = 0; stateID < State.SIZE; stateID++){
			// trellis[stateID][docSize] = trellis[stateID][docSize]; 
			psi[stateID][docSize] = lastMaxID;
		}

		int s= 26;
		mostLikelyPath[docSize+1] = s;
		for(int t=docSize; t>=0; t--){
			s = psi[s][t];
			mostLikelyPath[t] = s;
		}

		return mostLikelyPath;
	}
}


