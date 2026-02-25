package utopia.echo.model.request.vastai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.request.Body
import utopia.echo.model.vastai.instance.offer.OfferType.OnDemand
import utopia.echo.model.vastai.instance.offer.{Offer, OfferProperty, OfferType, SearchFilter}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

/**
 * Used for querying for available offers
 * @param allocatedStorageGigas Amount of allocated storage for the instance, in GB. Can't be adjusted later.
 *                              Default = 8 GB.
 * @param filters Applied filters. Default = empty.
 * @param order Applied ordering. Default = empty.
 * @param limit Maximum number of returned offers. Default = None = unlimited.
 * @param offerType Type of searched offers. Default = on-demand.
 * @param deprecationView A view that contains true if this request should be retracted (default = always false)
 * @param noBundling Whether to disable offer bundling
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class GetOffers(allocatedStorageGigas: Int = 8, filters: Seq[SearchFilter] = Empty,
                     order: Seq[(OfferProperty[_], Sign)] = Empty, limit: Option[Int] = None,
                     offerType: OfferType = OnDemand, deprecationView: View[Boolean] = AlwaysFalse,
                     noBundling: Boolean = false)
	extends ApiRequest[Seq[Offer]]
{
	// ATTRIBUTES   -----------------------
	
	override val method: Method = Post
	override val path: String = "bundles"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  -----------------------
	
	override def body: Either[Value, Body] = Left(Model.from(
		"type" -> offerType.key,
		"allocated_storage" -> allocatedStorageGigas,
		"order" -> order.map { case (prop, dir) =>
			Pair[Value](prop.key, dir match {
				case Positive => "asc"
				case Negative => "desc"
			})
		},
		"limit" -> limit,
		"disable_bundling" -> noBundling) ++ filters.view.map { _.toConstant })
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[Offer]]] =
		prepared.parseValue { body =>
			body.getModelOrVector match {
				case Left(body) => body.tryGet("offers") { _.tryVectorWith { _.tryModel.flatMap(Offer.apply) } }
				case Right(values) => values.tryMapAll { _.tryModel.flatMap(Offer.apply) }
			}
		}
}