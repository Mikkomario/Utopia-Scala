package utopia.reflection.component.swing

import javax.swing.JComponent
import utopia.genesis.color.Color

object JWrapper
{
    /**
     * Wraps a JComponent
     */
    def apply(component: JComponent): JWrapper = new SimpleJWrapper(component)
}

/**
* This is an extended version of Wrapper that allows access for JComponent functions
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait JWrapper extends AwtComponentWrapper with SwingComponentRelated
{
    override def background_=(color: Color) =
    {
        component.setBackground(color.toAwt)
        isTransparent_=(false)
    }
}

private class SimpleJWrapper(val component: JComponent) extends JWrapper