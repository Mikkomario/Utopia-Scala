package utopia.scribe.core.model.factory.management

import utopia.flow.util.Mutate

import java.time.Instant

/**
  * Common trait for classes that implement IssueAliasFactory by wrapping a IssueAliasFactory 
  * instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait IssueAliasFactoryWrapper[A <: IssueAliasFactory[A], +Repr] extends IssueAliasFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The factory wrapped by this instance
	  */
	protected def wrappedFactory: A
	
	/**
	  * Mutates this item by wrapping a mutated instance
	  * @param factory The new factory instance to wrap
	  * @return Copy of this item with the specified wrapped factory
	  */
	protected def wrap(factory: A): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def withAlias(alias: String) = mapWrapped { _.withAlias(alias) }
	
	override def withCreated(created: Instant) = mapWrapped { _.withCreated(created) }
	
	override def withIssueId(issueId: Int) = mapWrapped { _.withIssueId(issueId) }
	
	override def withNewSeverity(newSeverity: Int) = mapWrapped { _.withNewSeverity(newSeverity) }
	
	
	// OTHER	--------------------
	
	/**
	  * Modifies this item by mutating the wrapped factory instance
	  * @param f A function for mutating the wrapped factory instance
	  * @return Copy of this item with a mutated wrapped factory
	  */
	protected def mapWrapped(f: Mutate[A]) = wrap(f(wrappedFactory))
}

