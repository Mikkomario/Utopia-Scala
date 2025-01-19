package utopia.reach.test.interactive

import utopia.firmament.awt.AwtEventThread
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.time.TimeExtensions._
import utopia.reach.test.ReachTestContext._

import java.awt.Dimension
import javax.swing.{JFrame, JPanel}

/**
  *
  * @author Mikko Hilpinen
  * @since 19.01.2025, v
  */
object AcquireWindowInsetsTest extends App
{
	/*
	JFrame frame = new JFrame("Title Bar Height Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add a fixed-size panel to ensure known content size
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 200));
        frame.add(panel);

        // Pack the frame to fit the panel and decorations
        frame.pack();

        // Make the frame visible
        frame.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            // Calculate title bar height and border width
            int titleBarHeight = frame.getHeight() - panel.getHeight();
            int leftInset = (frame.getWidth() - panel.getWidth()) / 2;

            System.out.println("Title bar height: " + titleBarHeight);
            System.out.println("Left inset (border width): " + leftInset);
        });
	 */
	val frame = new JFrame("Test")
	
	// Force decorations off, then on// Force decorations off, then on
	frame.setUndecorated(true)
	frame.setUndecorated(false)
	
	val panel = new JPanel()
	panel.setPreferredSize(new Dimension(300, 200))
	frame.add(panel)
	frame.pack()
	
	frame.setLocationRelativeTo(null)
	frame.setVisible(true)
	
	Delay(1.seconds) {
		AwtEventThread.later {
			val h = frame.getHeight - 200
			val w = frame.getWidth - 300
			
			println(h)
			println(w)
			println(frame.getSize)
			println(panel.getSize)
			println(frame.getRootPane.getBounds)
			
			println(frame.getLocationOnScreen)
			println(frame.getContentPane.getLocationOnScreen)
			
			// frame.setLocation(400, 400)
			// println(frame.getLocation)
		}
		
		(1 to 5).foreach { i =>
			val y = 400 + i
			AwtEventThread.later { frame.setLocation(400, y) }
			AwtEventThread.later { println(s"Position is still correct: ${ frame.getY == y }") }
			Wait(0.5.seconds)
		}
	}
	
	Delay(5.seconds) {
		frame.setVisible(false)
		frame.dispose()
	}
}
