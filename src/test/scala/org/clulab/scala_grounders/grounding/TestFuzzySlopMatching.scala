package org.clulab.scala_grounders.grounding

import org.scalatest.{FlatSpec, Matchers, Tag}
import org.clulab.scala_grounders.model.DKG

/**
  * sbt "testOnly org.clulab.scala_grounders.grounding.TestFuzzySlopMatching"
  *
  */
class TestFuzzySlopMatching extends FlatSpec with Matchers {

  /**
    * These are the documents over which we ground our candidates
    * More explicitly, let us assume we want to ground a new document with name: "dog dog"
    * This document will be grounded to the DKGs with ids "id3" and "id4" (because of the fuzziness)
    * The fuzziness means that we check for inclusions as well (i.e., there can be gaps, it is not mandatory
    * to be "==", etc); However, it is mandatory for all words to be present (which is why we do not ground to the
    * document with "id1"))
    * 
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
      name        = "dog cat dog",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id4",
      name        = "dog dog",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id5",
      name        = "cat dog cat",
      description = None,
      synonyms    = Seq.empty,
    ),
  )

  val testDocument = "dog dog"

  val fieldGroups = Seq(
    Seq("name"),
    Seq("description")
  )

  val slops = Seq(1, 2, 3)

  behavior of "FuzzySlopGrounder"

  it should "return correct answer" in {
    val eg = new FuzzySlopGrounder(fieldGroups, slops)
    
    val result    = eg.ground(testDocument, None, groundingTargets, 1).toSeq
    val resultDkg = result.sortBy(_.score).map(_.dkg).distinct

    result.foreach(println)
    

    // Only two documents is acceptable
    resultDkg.length should be (2)

    // Check id
    resultDkg(0).id should be ("id3") // Because it should have a lower score, and we sorted
    resultDkg(1).id should be ("id4") // Because it should have a higher score, and we sorted

    // Check full DKG
    resultDkg(0) should be (groundingTargets(2)) // Because it should have a lower score, and we sorted
    resultDkg(1) should be (groundingTargets(3)) // Because it should have a higher score, and we sorted

    // Check details
    result(0).groundingDetails.grounderName should be ("Fuzzy Slop Grounder")
    result(1).groundingDetails.grounderName should be ("Fuzzy Slop Grounder")

  }

}