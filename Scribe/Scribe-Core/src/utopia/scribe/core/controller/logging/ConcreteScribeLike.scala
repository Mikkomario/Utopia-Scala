package utopia.scribe.core.controller.logging

/**
  * Common trait for concrete logger implementations on both the client and the server side
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  * @tparam Repr Type of this logging implementation
  */
// TODO: Add logToConsole and logToFile -options (to ScribeContext) once the basic features have been implemented
trait ConcreteScribeLike[+Repr] extends ScribeLike[Repr]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The context where this Scribe instance is applied.
	  *         Should be unique. E.g. The name of the feature this Scribe performs the logging for.
	  */
	protected def context: String
	/**
	  * @param context New context to assign
	  * @return Copy of this Scribe with the new context -property
	  */
	def withContext(context: String): Repr
	
	
	// IMPLEMENTED  -------------------------
	
	override def in(subContext: String): Repr = {
		if (subContext.isEmpty)
			self
		else {
			val c = context
			if (c.isEmpty)
				withContext(subContext)
			else {
				// Selects a separator appropriate for the current context
				val separator = {
					if (c.contains(' '))
						" "
					else if (c.contains('_'))
						"_"
					else
						"."
				}
				withContext(s"$c$separator$subContext")
			}
		}
	}
}
