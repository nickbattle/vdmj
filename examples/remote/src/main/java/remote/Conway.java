/**
 * This simple example is designed to run with the Conway.vdmsl test spec in
 * src/test/resources. It demonstrates the use of the RemoteControl interface
 * to display a GUI animation of the Game of Life.
 *
 * Execute in VDM by adding the project to the classpath and passing:
 *
 *     -remote remote.Comway
 *
 * Rather than giving an interpreter prompt, this will hand control over to the
 * GUI, which displays the Game board and increments it automatially. Close the
 * GUI window to finish.
 */

package remote;

import java.awt.Graphics;
import java.math.BigInteger;

import javax.swing.JFrame;

import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;

/**
 * Animation for the Game of Life VDM-SL model
 * @author Nick Battle
 */
public class Conway implements RemoteControl
{
	private final static int CELL = 10;		// Cell size in pixels
	private final static int SIZE = 500;	// Display size in pixels
	private final static String GAME = "GOSPER_GLIDER_GUN";
	private final static int PAUSE = 500;
	
	@Override
	public void run(RemoteInterpreter interpreter) throws Exception
	{
		Grid g = new Grid();
		ValueSet population = interpreter.valueExecute(GAME).setValue(null);
		g.setData(population);
		g.setVisible(true);
		
		while (g.isVisible())
		{
			Thread.sleep(PAUSE);
			population = interpreter.valueExecute("generation(" + population +")").setValue(null);
			g.setData(population);
			g.repaint();
		}
		
		g.dispose();
	}
	
	@SuppressWarnings("serial")
	private static class Grid extends JFrame
	{
		private ValueSet data;
		
		public Grid()
		{
			this.setSize(SIZE, SIZE);
			this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		}
		
		public void setData(ValueSet data)
		{
			this.data = data;
		}
		
		@Override
		public void paint(Graphics g)
		{
			g.clearRect(0, 0, SIZE, SIZE);
			g.translate(SIZE/2, SIZE/2);
			
			try
			{
				for (Value point: data)
				{
					RecordValue record = point.recordValue(null);
					Object xval = record.fieldmap.get("x").intValue(null);
					Object yval = record.fieldmap.get("y").intValue(null);
					
					int x = 0;
					int y = 0;
					
					if (xval instanceof BigInteger)
					{
						x = ((BigInteger)xval).intValue();
						y = ((BigInteger)yval).intValue();
					}
					else
					{
						x = ((Long)xval).intValue();
						y = ((Long)yval).intValue();
					}
					
					g.fillRect(x*CELL, -y*CELL, CELL-1, CELL-1);
				}
			}
			catch (ValueException e)
			{
				e.printStackTrace();
			}
		}
	}
}
