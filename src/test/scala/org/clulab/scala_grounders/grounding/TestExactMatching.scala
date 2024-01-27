package org.clulab.scala_grounders.grounding

import org.scalatest.{FlatSpec, Matchers, Tag}
import org.clulab.scala_grounders.model.DKG

/**
  * sbt "testOnly org.clulab.scala_grounders.grounding.TestExactMatching"
  *
  */
class TestExactMatching extends FlatSpec with Matchers {

  /**
    * These are the documents over which we ground our candidates
    * More explicitly, let us assume we want to ground a new documnet with name: "dog"
    * This document will be grounded to the first DKG below (since name="dog")
    * 
    * If we want to ground a new document with name "cute dog", no document will be grounded
    * The reason is that this grounder is an Exact Grounder. This means that it looks for 
    * perfect match (i.e. "==", not inclusion) to ground. If you are looking for other types of groundings,
    * please check the classes that implement `org.clulab.scala_grounders.grounding.Grounder`.
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
  )

  val testDocument = "dog"

  val fieldGroups = Seq(
    Seq("name"),
    Seq("description")
  )

  behavior of "ExactGrounder"

  it should "return correct answer" in {
    val eg = new ExactGrounder(fieldGroups)
    
    val result = eg.ground(testDocument, groundingTargets, 1).toSeq

    // Only one document is acceptable
    result.length should be (1)

    // Check id
    result.head.dkg.id should be ("id1")

    // Check full DKG
    result.head.dkg should be (groundingTargets.head)

    // Check details
    result.head.groundingDetails.grounderName should be ("Exact Grounder")

  }

}