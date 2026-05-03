package utopia.vigil.database.props.scope

/**
  * Common trait for interfaces that provide access to scope right database properties by 
  * wrapping a ScopeRightDbProps
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbPropsWrapper extends ScopeRightDbProps
{
	// ABSTRACT	--------------------
	
	/**
	  * The wrapped scope right database properties
	  */
	protected def scopeRightDbProps: ScopeRightDbProps
	
	
	// IMPLEMENTED	--------------------
	
	override def created = scopeRightDbProps.created
	
	override def id = scopeRightDbProps.id
	
	override def scopeId = scopeRightDbProps.scopeId
	
	override def usable = scopeRightDbProps.usable
}

