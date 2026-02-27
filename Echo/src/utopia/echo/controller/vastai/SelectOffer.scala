package utopia.echo.controller.vastai

import utopia.echo.model.vastai.instance.offer.OfferType.OnDemand
import utopia.echo.model.vastai.instance.offer.{Offer, OfferProperty, OfferType, SearchFilter}
import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.sign.Sign

import scala.concurrent.Future
import scala.util.Try

object SelectOffer
{
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
	 * @return Ordering to apply when requesting offers
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
