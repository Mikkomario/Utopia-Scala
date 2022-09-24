package utopia.citadel.database.access.many.description

import utopia.citadel.util.CitadelContext
import utopia.flow.datastructure.mutable.RefreshingLazy
import utopia.flow.time.TimeExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.template.LazyLike
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.util.ErrorHandling

import scala.concurrent.duration.Duration

/**
  * The root access point when targeting multiple DescriptionRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbDescriptionRoles extends ManyDescriptionRolesAccess with UnconditionalView
{
	// ATTRIBUTES   ----------------
	
	// Caches all description roles, since they are needed relatively often and change rarely
	private lazy val cache: Option[LazyLike[Vector[DescriptionRole]]] =
	{
		val cacheDuration = CitadelContext.descriptionRoleCacheDuration
		if (cacheDuration <= Duration.Zero)
			None
		else
		{
			import CitadelContext.executionContext
			import CitadelContext.connectionPool
			
			def _readValues = connectionPool.tryWith { implicit c => super.pull }
				.getOrMap { error =>
					ErrorHandling.defaultPrinciple.handle(error)
					Vector()
				}
			
			if (cacheDuration > 1.minutes)
				Some(RefreshingLazy.after(cacheDuration)(_readValues))
			else
				Some(ExpiringLazy.after(cacheDuration)(_readValues))
		}
	}
	
	
	// IMPLEMENTED  ------------------
	
	override def ids(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value.map { _.id }
		case None => super.ids
	}
	override def size(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value.size
		case None => super.size
	}
	override def all(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value
		case None => super.all
	}
	override def foreach[U](f: DescriptionRole => U)(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value.foreach(f)
		case None => super.foreach(f)
	}
	override def pull(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value
		case None => super.pull
	}
	override def nonEmpty(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value.nonEmpty
		case None => super.nonEmpty
	}
	override def isEmpty(implicit connection: Connection) = cache match
	{
		case Some(c) => c.value.isEmpty
		case None => super.isEmpty
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted DescriptionRoles
	  * @return An access point to DescriptionRoles with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbDescriptionRolesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbDescriptionRolesSubset(override val ids: Set[Int]) 
		extends ManyDescriptionRolesAccess 
			with ManyDescribedAccessByIds[DescriptionRole, DescribedDescriptionRole]
}

