package org.clulab.scala_grounders.grounding

import org.scalatest.{FlatSpec, Matchers, Tag}
import org.clulab.scala_grounders.model.DKG

/**
  * sbt "testOnly org.clulab.scala_grounders.grounding.TestFuzzyEditDistanceMatching"
  *
  */
class TestFuzzyEditDistanceMatching extends FlatSpec with Matchers {

  /**
    * These are the documents over which we ground our candidates
    * More explicitly, let us assume we want to ground a new documnet with name: "dog dog"
    * This document will be grounded to the DKGs with ids "id3" and "id4" (because of the fuzziness)
    * The fuzziness means that we check for inclusions as well (i.e., there can be gaps, it is not mandatory
    * to be "==", etc); However, it is mandatory for all words to be present (which is why we do not ground to the
    * document with "id1"))
    * 
    */
  val groundingTargets = Seq(
    new DKG(
      id          = "id1",
      name        = "dogg",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id2",
      name        = "doggg",
      description = None,
      synonyms    = Seq.empty,
    ),
    new DKG(
      id          = "id3",
      name        = "cat",
      description = None,
      synonyms    = Seq.empty,
    ),
  )

  val testDocument = "dog"

  val fieldGroups = Seq(
    Seq("name"),
    // Seq("description")
  )


  behavior of "FuzzyEditDistanceGrounder with edit distance of 1 2"

  it should "return correct answer when edit distance is 1" in {
    val eg = new FuzzyEditDistanceGrounder(fieldGroups, maxEditDistance = 1)
    
    val result    = eg.ground(testDocument, None, groundingTargets, 1).toSeq
    val resultDkg = result.sortBy(_.score).map(_.dkg).distinct

    result.foreach(println)
    

    // Only one document is acceptable
    resultDkg.length should be (1)

    // Check id
    resultDkg(0).id should be ("id1") // Because the edit distance for the others is larger

    // Check full DKG
    resultDkg(0) should be (groundingTargets(0)) // Because it should have a lower score, and we sorted

    // Check details
    result(0).groundingDetails.grounderName should be ("Fuzzy Edit Distance Grounder")

  }

  it should "return correct answer when edit distance is 2" in {
    val eg = new FuzzyEditDistanceGrounder(fieldGroups, maxEditDistance = 2)
    
    val result    = eg.ground(testDocument, None, groundingTargets, 1).toSeq
    val resultDkg = result.sortBy(-_.score).map(_.dkg).distinct

    result.foreach(println)
    

    // Only one document is acceptable
    resultDkg.length should be (2)

    // Check id
    resultDkg(0).id should be ("id1") 
    resultDkg(1).id should be ("id2") 

    // Check full DKG
    resultDkg(0) should be (groundingTargets(0)) 
    resultDkg(1) should be (groundingTargets(1)) 

    // Check details
    result(0).groundingDetails.grounderName should be ("Fuzzy Edit Distance Grounder")
    result(1).groundingDetails.grounderName should be ("Fuzzy Edit Distance Grounder")

  }

  // it should "return correct answer when edit distance is 3" in {
  //   val eg = new FuzzyEditDistanceGrounder(fieldGroups, maxEditDistance = 3)
    
  //   val result    = eg.ground(testDocument, groundingTargets, 1).toSeq
  //   val resultDkg = result.sortBy(-_.score).map(_.dkg).distinct

  //   result.foreach(println)
    

  //   // Only one document is acceptable
  //   resultDkg.length should be (3)

  //   // Check id
  //   resultDkg(0).id should be ("id1") 
  //   resultDkg(1).id should be ("id2") 
  //   resultDkg(1).id should be ("id3") 

  //   // Check full DKG
  //   resultDkg(0) should be (groundingTargets(0))
  //   resultDkg(1) should be (groundingTargets(1))
  //   resultDkg(2) should be (groundingTargets(2))

  //   // Check details
  //   result(0).groundingDetails.grounderName should be ("Fuzzy Edit Distance Grounder")
  //   result(1).groundingDetails.grounderName should be ("Fuzzy Edit Distance Grounder")
  //   result(2).groundingDetails.grounderName should be ("Fuzzy Edit Distance Grounder")

  // }

}