package utopia.genesis.handling

import utopia.genesis.event.MouseDragEvent
import utopia.inception.handling.HandlerType
import utopia.inception.util.Filter

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
case object MouseDragHandlerType extends HandlerType
{
	override lazy val supportedClass: Class[_] = classOf[MouseDragListener]
}

/**
 * A handler used for distributing mouse drag events
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2.1
 */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait MouseDragHandler extends EventHandler[MouseDragListener, MouseDragEvent] with MouseDragListener
{
	override def handlerType = MouseDragHandlerType
	
	override protected def eventFilterFor(listener: MouseDragListener): Filter[MouseDragEvent] =
		listener.mouseDragFilter
	
	override protected def inform(listener: MouseDragListener, event: MouseDragEvent): Any =
		listener.onMouseDrag(event)
	
	override def onMouseDrag(event: MouseDragEvent): Unit = distribute(event)
}
