/**
 * RDDL: A simple graphics display for the Sidewalk domain. 
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 * @version 10/10/10
 *
 **/

package rddl.viz;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;

import rddl.EvalException;
import rddl.State;
import rddl.RDDL.LCONST;
import rddl.RDDL.PVARIABLE_DEF;
import rddl.RDDL.PVAR_NAME;
import rddl.RDDL.TYPE_NAME;

public class PacPeopleDisplay extends StateViz {
	
	public PacPeopleDisplay() {
		_nTimeDelay = 200; // in milliseconds
	}

	public PacPeopleDisplay(int time_delay_per_frame) {
		_nTimeDelay = time_delay_per_frame; // in milliseconds
	}
	
	public boolean _bSuppressNonFluents = true;
	public BlockDisplay _bd = null;
	public int _nTimeDelay = 0;
	public int _maxRow = -1;
	public int _maxCol = -1;
	
	public void display(State s, int time) {
		try {
			System.out.println("TIME = " + time + ": " + getStateDescription(s));
		} catch (EvalException e) {
			System.out.println("\n\nError during visualization:\n" + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	//////////////////////////////////////////////////////////////////////

	public String getStateDescription(State s) throws EvalException {
		StringBuilder sb = new StringBuilder();

		TYPE_NAME xpos_type 		= new TYPE_NAME("xpos");
		ArrayList<LCONST> list_xpos = s._hmObject2Consts.get(xpos_type);

		TYPE_NAME ypos_type 		= new TYPE_NAME("ypos");
		ArrayList<LCONST> list_ypos = s._hmObject2Consts.get(ypos_type);
		
		TYPE_NAME pacperson_type 		 = new TYPE_NAME("pacperson");
		ArrayList<LCONST> list_pacperson = s._hmObject2Consts.get(pacperson_type);
		
		PVAR_NAME SOLID_CELL 		= new PVAR_NAME("SOLID-CELL");
		PVAR_NAME pellet 			= new PVAR_NAME("pellet-at-cell");
		PVAR_NAME pacperson_at_cell	= new PVAR_NAME("pacperson-at-cell");

		if (_bd == null) {
			_maxRow = list_ypos.size() - 1;
			_maxCol = list_xpos.size() - 1;

			_bd= new BlockDisplay("RDDL PacPeople Simulation", "RDDL PacPeople Simulation", _maxRow + 2, _maxCol + 2);	
		}
		
		// Set up an arity-2 and arity-3 parameter lists
		ArrayList<LCONST> params2 = new ArrayList<LCONST>(2);
		ArrayList<LCONST> params3 = new ArrayList<LCONST>(3);
		
		for (int i = 0; i < 2; i++) {
			params2.add(null);
			params3.add(null);
		}
		params3.add(null);

		_bd.clearAllCells();
		_bd.clearAllLines();
		_bd.clearAllCircles();
		_bd.clearAllText();
		
		for (LCONST xpos : list_xpos) {
			for (LCONST ypos : list_ypos) {
				int col = new Integer(xpos.toString().substring(2, xpos.toString().length())) - 1;
				int row = new Integer(ypos.toString().substring(2, ypos.toString().length())) - 1;
				row = _maxRow - row + 1;
				
				params2.set(0, xpos);
				params2.set(1, ypos);
				params3.set(1, xpos);
				params3.set(2, ypos);
				
				boolean b_is_solid_cell = (Boolean)s.getPVariableAssign(SOLID_CELL, params2);
				boolean b_pellet		= (Boolean)s.getPVariableAssign(pellet, params2);
				
				boolean b_pacperson = false;
				
				// Check if any pacperson is at (xpos, ypos)
				for (LCONST pacperson : list_pacperson) {
					params3.set(0, pacperson);
					if ((Boolean)s.getPVariableAssign(pacperson_at_cell, params3))
						b_pacperson = true;
				}
				
				String letter = null;
				
				if (b_pacperson) {
					Color pacpersonColor = Color.orange;
					_bd.addCircle(pacpersonColor, col+0.5, row+0.5, 0.3);
				} else if (b_pellet && !b_is_solid_cell) {
					Color pelletColor = Color.yellow;
					_bd.addCircle(pelletColor, col+0.5, row+0.5, 0.1);
				} 
				
				Color color = Color.gray;
//				
				if (b_is_solid_cell)
//					color = new Color(139, 69, 19); // brown
					color = Color.black;
								
				_bd.setCell(row, col, color, letter);
			}
		}
			
		_bd.repaint();
		
		// Go through all variable types (state, interm, observ, action, nonfluent)
		for (Map.Entry<String,ArrayList<PVAR_NAME>> e : s._hmTypeMap.entrySet()) {
			
			if (_bSuppressNonFluents && e.getKey().equals("nonfluent"))
				continue;
			
			// Go through all variable names p for a variable type
			for (PVAR_NAME p : e.getValue()) 
				try {
					// Go through all term groundings for variable p
					ArrayList<ArrayList<LCONST>> gfluents = s.generateAtoms(p);										
					for (ArrayList<LCONST> gfluent : gfluents)
						sb.append("- " + e.getKey() + ": " + p + 
								(gfluent.size() > 0 ? gfluent : "") + " := " + 
								s.getPVariableAssign(p, gfluent) + "\n");
						
				} catch (EvalException ex) {
					sb.append("- could not retrieve assignment" + s + " for " + p + "\n");
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

