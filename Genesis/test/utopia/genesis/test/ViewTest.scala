package utopia.genesis.test

import utopia.genesis.view.MainFrame
import javax.swing.JPanel
import utopia.genesis.shape.shape2D.Size

object ViewTest extends App
{
    val frame = new MainFrame(new JPanel(), Size(640, 480), "ViewTest")
    frame.display()
    
    println("Success")
    
    /* TODO: Here's a clip that should fix windows 8+ resize issue for frame
    masterWindow.addComponentListener(new ComponentAdapter() {
      private int oldWidth = 0;
      private int oldHeight = 0;

      @Override
      public void componentResized(ComponentEvent e) {
        oldWidth = masterWindow.getWidth();
        oldHeight = masterWindow.getHeight();
      }

      @Override
      public void componentMoved(ComponentEvent e) {
          if (masterWindow.getWidth() != oldWidth || masterWindow.getHeight() != oldHeight) {
            masterWindow.invalidate();
            masterWindow.validate();
          }
          oldWidth = masterWindow.getWidth();
          oldHeight = masterWindow.getHeight();
      }
    });
     */
}