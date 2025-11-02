package utopia.reflection.container.template

import utopia.reflection.component.template.ReflectionComponentLike

/**
  * A common trait for both mutable and immutable component containers / hierarchies
  * @author Mikko Hilpinen
  * @since 13.3.2020, v1
  */
trait ContainerLike[+C <: ReflectionComponentLike] extends ReflectionComponentLike
{
	// ABSTRACT    ----------------
	
	/**
	  * The current components in this container
	  */
	def components: Seq[C]
	
	
	// IMPLEMENTED	----------------
	
	override def toString = {
		val contentIter = components.iterator
		val contentStrBuilder = new StringBuilder()
		var contentLength = 0
		
		while (contentLength < 40 && contentIter.hasNext) {
			val nextStr = contentIter.next().toString
			if (contentLength != 0)
				contentStrBuilder ++= ", "
			contentStrBuilder ++= nextStr
			contentLength += nextStr.length
		}
		val contentStr = {
			if (contentLength > 40)
				s"${ components.size } components"
			else
				s"[${ contentStrBuilder.result() }]"
		}
		
		s"${getClass.getSimpleName}($contentStr)"
	}
}
