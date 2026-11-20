package utopia.echo.controller.vastai

import utopia.echo.model.vastai.instance.offer.OfferProperty.CostPerHour
import utopia.echo.model.vastai.instance.offer.OfferType.OnDemand
import utopia.echo.model.vastai.instance.offer.{Offer, OfferProperty, OfferType, SearchFilter}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.operator.sign.Sign

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

object SelectOffer
{
	// ATTRIBUTES   -------------------
	
	/**
	 * A factory for constructing SelectOffer instances.
	 * Applies the default settings.
	 */
	val factory = SelectOfferFactory()
	
	
	// IMPLICIT -----------------------
	
	// Implicitly treats this object as a factory
	implicit def objectAsFactory(o: SelectOffer.type): SelectOfferFactory = o.factory
	
	
	// OTHER    -----------------------
	
	/**
	 * Creates a new offer selection logic
	 * @param filters Filters to apply when requesting offers. Default = empty.
	 * @param ordering Ordering to apply when requesting offers. Default = empty.
	 * @param limit Maximum number of offers requested. None if unlimited (default).
	 * @param offerType Type of offers looked for. Default = on-demand.
	 * @param select A function that receives n orders and selects the best one.
	 *               Yields a future that resolves into either the selected offer, or into a failure.
	 * @return Select offer logic using the specified function & info.
	 */
	def apply(filters: Seq[SearchFilter] = Empty, ordering: Seq[(OfferProperty[_], Sign)] = Empty,
	          limit: Option[Int] = None, offerType: OfferType = OnDemand)
	         (select: Seq[Offer] => Future[Try[Offer]]): SelectOffer =
		new _SelectOffer(offerType, filters, ordering, limit)(select)
	
	
	// NESTED   -----------------------
	
	case class SelectOfferFactory(filters: Seq[SearchFilter] = Empty, ordering: Seq[(OfferProperty[_], Sign)] = Empty,
	                              limit: Option[Int] = None, offerType: OfferType = OnDemand,
	                             // By default, selects the first offer
	                              select: Seq[Offer] => Future[Try[Offer]] = { offers =>
		                              Future.successful(offers.headOption.toTry {
			                              new IllegalStateException("No offers to select from")
		                              })
	                              })
	{
		/**
		 * @return Copy of this factory only receiving the top offer
		 */
		def head = take(1)
		/**
		 * @return A copy of this factory that orders by cost (ascending)
		 */
		def cheapestFirst = orderBy(CostPerHour, ascending = true)
		
		/**
		 * @param filter An additional search filter to apply
		 * @return Copy of this factory applying the specified search filter
		 */
		def +=(filter: SearchFilter) = copy(filters = filters :+ filter)
		/**
		 * @param filter An additional search filter to apply
		 * @param otherFilters Other filters to apply, also
		 * @return Copy of this factory applying the specified search filters
		 */
		def filter(filter: SearchFilter, otherFilters: SearchFilter*): SelectOfferFactory =
			copy(filters = OptimizedIndexedSeq.concat(filters, Single(filter), otherFilters))
		
		/**
		 * Applies ordering
		 * @param property Property to order by
		 * @param ascending Whether to apply ascending order
		 * @param overwrite Whether to overwrite previously set ordering (default = false = ordering will be extended)
		 * @return Copy of this factory applying the specified ordering
		 */
		def orderBy(property: OfferProperty[_], ascending: Boolean, overwrite: Boolean = false): SelectOfferFactory = {
			val newOrdering = property -> Sign(positiveCondition = ascending)
			copy(ordering = if (overwrite) Single(newOrdering) else ordering :+ newOrdering)
		}
		
		/**
		 * Limits the number of received orders
		 * @param count Number of orders to receive at most
		 * @return Copy of this factory applying the specified limit
		 */
		def take(count: Int): SelectOfferFactory = copy(limit = Some(limit match {
			case Some(prev) => prev min count
			case None => count
		}))
		
		/**
		 * @param f A function to use for selecting from the pulled offers.
		 *          Receives the pulled offers (which may be empty).
		 *          Yields a future that either succeeds with the selected order, or fails,
		 *          terminating the parent process.
		 * @return Copy of this factory applying the specified selection logic
		 */
		def selectUsing(f: Seq[Offer] => Future[Try[Offer]]) = copy(select = f)
		
		/**
		 * @return SelectOffer logic based on the specified factory settings
		 */
		def apply(): SelectOffer = new _SelectOffer(offerType, filters, ordering, limit)(select)
	}
	
	private class _SelectOffer(override val offerType: OfferType, override val filters: Seq[SearchFilter],
	                           override val ordering: Seq[(OfferProperty[_], Sign)], override val limit: Option[Int])
	                          (f: Seq[Offer] => Future[Try[Offer]])
		extends SelectOffer
	{
		override def apply(offers: Seq[Offer]): Future[Try[Offer]] = f(offers)
	}
}

/**
 * Common trait for offer selection logic implementations
 * @author Mikko Hilpinen
 * @since 27.02.2026, v1.5
 */
trait SelectOffer
{
	/**
	 * @return Type of offers looked for
	 */
	def offerType: OfferType
	/**
	 * @return Filters to apply when requesting offers
	 */
	def filters: Seq[SearchFilter]
	/**
	 * @return Ordering to apply when requesting offers.
	 *         The sign after each property is interpreted as:
	 *         - Positive = Ascending
	 *         - Negative = Descending
	 */
	def ordering: Seq[(OfferProperty[_], Sign)]
	/**
	 * @return Maximum number of offers requested. None if unlimited.
	 */
	def limit: Option[Int]
	
	/**
	 * Selects the best offer
	 * @param offers Offers to select from
	 * @return A future that resolves into the selected offer, or into a failure
	 */
	def apply(offers: Seq[Offer]): Future[Try[Offer]]
}
