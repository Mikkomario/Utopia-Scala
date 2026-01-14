package utopia.flow.test.collection

import utopia.flow.collection.mutable.builder.CompoundingSeqBuilder

/**
 *
 * @author Mikko Hilpinen
 * @since 13.01.2026, v
 */
object SelfReferencingBuilderTest extends App
{
	private val newNames = Vector(
		"National Airline Company Belavia Open Joint Stock Company",
		"Belavia",
		"National Airline Company Belavia OJSC",
		"Belavia-Belarusian Airlines",
		"National Airline Company Belavia Joint Stock Company",
		"Open Joint Stock Company Belavia Belarusian Airlines",
		"Aviakompaniya Belavia",
		"Aviy",
		"mpaniya Belaviya",
		"Natsyyanalnaya aviy",
		"mapniya Belaviya",
		"Natsionalnaya aviakompaniya Belavia",
		"Belavia Belarusian Airlines"
	)
	private val existingNames = Vector(
		"Aircompany Grodno Open Joint Stock Company",
		"Aircompany Grodno",
		"Aircompany Grodno OJSC",
		"Aeroport Grodno",
		"Grodnenskiy obedinenniy aviaotryad",
		"Aviakompaniya Grodno",
		"Aviy",
		"mpaniya Grodna"
	)
	
	private val builder = new CompoundingSeqBuilder[String](existingNames)
	builder ++= newNames.iterator.filterNot(builder.contains)
	
	assert(builder.result() == Vector(
		"Aircompany Grodno Open Joint Stock Company",
		"Aircompany Grodno",
		"Aircompany Grodno OJSC",
		"Aeroport Grodno",
		"Grodnenskiy obedinenniy aviaotryad",
		"Aviakompaniya Grodno",
		"Aviy",
		"mpaniya Grodna",
		"National Airline Company Belavia Open Joint Stock Company",
		"Belavia",
		"National Airline Company Belavia OJSC",
		"Belavia-Belarusian Airlines",
		"National Airline Company Belavia Joint Stock Company",
		"Open Joint Stock Company Belavia Belarusian Airlines",
		"Aviakompaniya Belavia",
		"mpaniya Belaviya",
		"Natsyyanalnaya aviy",
		"mapniya Belaviya",
		"Natsionalnaya aviakompaniya Belavia",
		"Belavia Belarusian Airlines"
	))
	
	println("Done!")
}
