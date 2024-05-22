package rddl.viz;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class AmbulanceDisplay extends StateViz {
	
	public AmbulanceDisplay() {
		_nTimeDelay = 200;
	}
	
	public AmbulanceDisplay(int time_delay_per_frame) {
		_nTimeDelay = time_delay_per_frame;
	}
	
	public boolean _bSuppressNonFluents = false;
	public BlockDisplay _bd = null;
	public int _nTimeDelay = 0;
	public int _maxCol = -1;
	public int _maxRow = -1;
	
	public void display(State s, int time) {
		try {
			System.out.println("TIME = " + time + ": " + getStateDescription(s));
		} catch (EvalException e) {
			System.out.println("\n\nError during visualization:\n"+e);
			e.printStackTrace();
			System.exit(1);
		}
	}
	/////////////////////////////////////////////////////
	
	public String getStateDescription(State s) throws EvalException {
		StringBuilder sb = new StringBuilder();
		
		TYPE_NAME ambulance_type = new TYPE_NAME("ambulance");
		ArrayList<LCONST> ambulances = s._hmObject2Consts.get(ambulance_type);
		
		TYPE_NAME xpos_type = new TYPE_NAME("xpos");
		ArrayList<LCONST> xpositions = s._hmObject2Consts.get(xpos_type);
		
		TYPE_NAME ypos_type = new TYPE_NAME("ypos");
		ArrayList<LCONST> ypositions = s._hmObject2Consts.get(ypos_type);

		PVAR_NAME ambulanceAt = new PVAR_NAME("ambulanceAt");
		PVAR_NAME hospital = new PVAR_NAME("HOSPITAL");
		PVAR_NAME patientOn = new PVAR_NAME("patientOn");
		PVAR_NAME active = new PVAR_NAME("activeCallAt");
//		PVAR_NAME moveEast = new PVAR_NAME("moveEast");
//		PVAR_NAME moveWest = new PVAR_NAME("moveWest");
//		PVAR_NAME moveNorth = new PVAR_NAME("moveNorth");
//		PVAR_NAME moveSouth = new PVAR_NAME("moveSouth");
//		PVAR_NAME pickPatient = new PVAR_NAME("pickPatient");
		
		// When initialized for the first time, get the size of the grid
		if (_bd == null) {
			for (int i = 0; i < xpositions.size(); i++) {
				LCONST xpos = xpositions.get(i);
				
				String xStr = xpos.toString().substring(2, xpos.toString().length());
				int x = Integer.parseInt(xStr);
				if (x > _maxCol) _maxCol = x;
			}
			
			for (int i = 0; i < ypositions.size(); i++) {
				LCONST ypos = ypositions.get(i);

				String yStr = ypos.toString().substring(2, ypos.toString().length());
				int y = Integer.parseInt(yStr);
				if (y > _maxRow) _maxRow = y;
			}
						
			_bd = new BlockDisplay("RDDL Ambulance Simulation", "Simulation", 2 * _maxRow + 1, 2 * _maxCol + 1);
		}
		
		// Set up an arity-1, arity-2 and arity-3 parameter list
		ArrayList<LCONST> params1 = new ArrayList<LCONST>(1);
		params1.add(null);
		ArrayList<LCONST> params2 = new ArrayList<LCONST>(2);
		params2.add(null);
		params2.add(null);
		ArrayList<LCONST> params3 = new ArrayList<LCONST>(3);
		params3.add(null);
		params3.add(null);
		params3.add(null);
		
		_bd.clearAllText();
		
		// Plot all grid points with corresponding states
		for (LCONST xpos : xpositions) {
			
			String xStr = xpos.toString().substring(2, xpos.toString().length());
			int x_location = Integer.parseInt(xStr) - 1;
			
			for (LCONST ypos : ypositions) {
				
				String yStr = ypos.toString().substring(2, ypos.toString().length());				
				int y_location = (_maxRow - Integer.parseInt(yStr));
				
				params2.set(0, xpos);
				params2.set(1, ypos);
				
				// Check if hospital or not, and get state values
				boolean location_is_hospital	= (Boolean)s.getPVariableAssign(hospital, params2);
				boolean is_active 				= (Boolean)s.getPVariableAssign(active, params2);
				
				for (int j = 0; j < ambulances.size(); j++) {
					LCONST ambulance = ambulances.get(j);
					params1.set(0, ambulance);
					params3.set(0, ambulance);
					params3.set(1, xpos);
					params3.set(2, ypos);
					
					boolean check_patient	 	= (Boolean)s.getPVariableAssign(patientOn, params1);
					boolean is_ambulanceAt		= (Boolean)s.getPVariableAssign(ambulanceAt, params3);
					
					if (is_ambulanceAt) {
						int x_ambulance = x_location;
						int y_ambulance = y_location;
						
						String amb_letter = null;
						Color amb_color = _bd._colors[11];			// red color
						if (check_patient) {
							amb_letter = "F";
						} else {
							amb_letter = "E";
						}
						
						// Display on screen
						_bd.addText(amb_color, 2*x_ambulance, 2*y_ambulance+1, amb_letter);
					} 
				}
				
				String letter = null;
				Color color = _bd._colors[9];		// grid points have dark-gray color
				if (location_is_hospital) {			// Hospitals have green color with "+" sign
					letter = "+";
					color = Color.green;
				} else if (is_active) {
					letter = "!";					// when there is a pending emergency call
					color = Color.magenta;			// the sign is '!' with magenta color.
				} else {
					letter = "o";
				}
				
				// Display on screen
				_bd.addText(color, 2*x_location+1, 2*y_location+1, letter);
			}
		}
					
			
		_bd.repaint();
		sb.append("\n");
 
		for (Map.Entry<String, ArrayList<PVAR_NAME>> entry : s._hmTypeMap.entrySet()) {
			
			if (entry.getKey().equals("nonfluent"))
				continue;
			
			for (PVAR_NAME p : entry.getValue()) {
				if ( !(p.toString().equals("ambulanceAt") | 
						p.toString().contains("move") | 
						p.toString().equals("pickPatient") | 
						p.toString().equals("activeCallAt") | 
						p.toString().equals("patientOn")))
					continue;
			
				try {
					ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);
					for (ArrayList<LCONST> gfluent : gfluents) {
						if ( (p.toString().contains("move") | p.toString().equals("pickPatient")) && (!(Boolean)s.getPVariableAssign(p, gfluent)) ) 
								continue;
						
						sb.append(entry.getKey() + ": " + p + 
								(gfluent.size() > 0 ? gfluent : "") + " := " +
								s.getPVariableAssign(p, gfluent) + "\n");
					}
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment" + s + " for " + p + "\n");
				}
			}
		}
		// Sleep so the animation can be viewed at a frame rate of 1000/_nTimeDelay per second
	    try {
			Thread.currentThread().sleep(_nTimeDelay);
		} catch (InterruptedException e) {
			System.err.println(e);
			e.printStackTrace(System.err);
		}
				
		return sb.toString();
	}
	
	public void close() {
		_bd.close();
	}
}

