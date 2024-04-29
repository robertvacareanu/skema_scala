package org.clulab.scala_grounders.grounding

import org.scalatest.{FlatSpec, Matchers, Tag}
import org.clulab.scala_grounders.model.DKG
import com.typesafe.config.ConfigFactory

/**
  * sbt "testOnly org.clulab.scala_grounders.grounding.TestNeuralMatching"
  *
  */
class TestNeuralMatching extends FlatSpec with Matchers {

  /**
    * These are the documents over which we ground our candidates
    * More explicitly, let us assume we want to ground a new document with name: "shiba inu"
    * The document with the highest score should be the first DKG below (since name="dog")
    */
  val groundingTargets = Seq(
    new DKG(
      id          = "id1",
      name        = "dog",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id2",
      name        = "cat",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id3",
      name        = "dog cat",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id4",
      name        = "cat",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id5",
      name        = "crocodile",
      description = None,
      synonyms    = Seq.empty,
    ),
  )

//   val testDocument = "Shiba Inu"
  val testDocuments = Seq(
    "Shiba Inu\nAnimal",
    // "Doggo\nAnimal",
    // "shiba inu\nAnimal",
    // "doggo\nAnimal",
  )

  val fieldGroups = Seq(
    Seq("name"),
    Seq("description")
  )

  behavior of "NeuralGrounder"

  it should "return correct answer" in {
    val config = ConfigFactory.load().getConfig("grounder")
    val componentNumber = Stream.from(1).filter(it => config.getConfig(f"behavior.sieve.component${it}").hasPath("modelPath")).headOption

    assert(componentNumber.isDefined)
    
    val eg = new NeuralGrounder(modelPath = config.getConfig(f"behavior.sieve.component${componentNumber.get}").getString("modelPath"), threshold = 0.000005)
    
    testDocuments.foreach { testDocument => 
        println("-"*20)
        println(testDocument)
        val result    = eg.ground(testDocument, None, groundingTargets, 1).toSeq
        val resultDkg = result.sortBy(-_.score).map(_.dkg).distinct.take(1)

        result.foreach(println)
        

        // // Only two documents is acceptable
        // resultDkg.length should be (1)

        // // Check id
        // resultDkg(0).id should be ("id1")

        // // Check full DKG
        // resultDkg(0) should be (groundingTargets(0))

        // // Check details
        // result(0).groundingDetails.grounderName should be ("Neural Grounder")
   
        // println("-"*20)
    }
  }

}