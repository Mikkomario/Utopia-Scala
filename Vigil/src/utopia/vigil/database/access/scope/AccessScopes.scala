package utopia.vigil.database.access.scope

import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many._
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.JoinType
import utopia.vigil.database.VigilTables
import utopia.vigil.database.access.scope.relation.{AccessScopeRelations, FilterByScopeRelation}
import utopia.vigil.database.access.token.scope.FilterByTokenScope
import utopia.vigil.database.reader.scope.ScopeDbReader
import utopia.vigil.database.storable.scope.ScopeRelationDbModel
import utopia.vigil.model.response.ResponseScope
import utopia.vigil.model.stored.scope.Scope

object AccessScopes 
	extends WrapRowAccess[AccessScopeRows] with WrapOneToManyAccess[AccessCombinedScopes] 
		with AccessManyRoot[AccessScopeRows[Scope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = apply(ScopeDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingManyRows[A]) = AccessScopeRows(access)
	override def apply[A](access: TargetingMany[A]) = AccessCombinedScopes(access)
	
	
	// OTHER    ------------------------
	
	def resolveRelationsToResponseModels(links: Seq[Pair[Int]], visitedScopeIds: Set[Int])
	                                    (implicit connection: Connection): Map[Int, Seq[ResponseScope]] =
	{
		// Case: No more links to resolve => Finishes
		if (links.isEmpty)
			Map()
		else {
			// Pulls the referenced scopes
			val nextScopeById = AccessScopes(links.view.map { _.second }).toMapBy { _.id }
			// Pulls links to resolve for this next layer
			val nextLinks = AccessScopeRelations.withParents((nextScopeById -- visitedScopeIds).keys)
				.parentAndChildIds
			
			val fullScopeById = {
				// Case: This is the last layer => Wraps the pulled scopes
				if (nextLinks.isEmpty)
					nextScopeById.view.mapValues { scope => ResponseScope(scope.id, scope.key) }.toMap
				// Case: There are more layers => Pulls the lower layers recursively
				else {
					val referenced = resolveRelationsToResponseModels(nextLinks, visitedScopeIds ++ nextScopeById.keys)
					nextScopeById.view
						.mapValues { scope =>
							ResponseScope(scope.id, scope.key, referenced.getOrElse(scope.id, Empty))
						}
						.toMap
				}
			}
			// Resolves the links
			links.groupMap { _.first } { p => fullScopeById(p.second) }
		}
	}
	
	
	// EXTENSIONS   ------------------
	
	implicit class RichAccessScopes(val a: AccessScopeRows[Scope]) extends AnyVal
	{
		/**
		 * @param connection Implicit DB connection
		 * @return All accessible scopes as response models (i.e. including granted scopes)
		 */
		def pullResponseModels(implicit connection: Connection) = {
			val roots = a.pull
			if (roots.isEmpty)
				Empty
			else {
				val rootIds = roots.view.map { _.id }.toSet
				val resolvedChildren = resolveRelationsToResponseModels(
					AccessScopeRelations.withParents(rootIds).parentAndChildIds, rootIds)
				
				roots.map { scope => ResponseScope(scope.id, scope.key, resolvedChildren.getOrElse(scope.id, Empty)) }
			}
		}
	}
}

/**
  * Used for accessing multiple scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
abstract class AccessScopes[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessScope[A]] with HasValues[AccessScopeValues] 
		with FilterScopes[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessScopeValues(wrapped)
	
	lazy val joinParentLinks = join(ScopeRelationDbModel.grantedScopeId.column)
	lazy val whereParentLinks = FilterByScopeRelation(joinParentLinks)
	
	lazy val joinTokenLinks = join(VigilTables.tokenScope)
	lazy val whereTokenLinks = FilterByTokenScope(joinTokenLinks)
	
	/**
	 * @return Access to root level scopes (i.e. scopes not nested under any other scope)
	 */
	def root = join(Single(ScopeRelationDbModel.grantedScopeId.column), JoinType.Right)
		.filter { ScopeRelationDbModel.parentScopeId.isNull }
}

/**
  * Provides access to row-specific scope -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessScopeRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessScopes[A, AccessScopeRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessScopeRows[A], AccessScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessScopeRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessScope(target)
}

/**
  * Used for accessing scope items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessCombinedScopes[A](wrapped: TargetingMany[A]) 
	extends AccessScopes[A, AccessCombinedScopes[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedScopes[A], AccessScope[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedScopes(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessScope(target)
}

