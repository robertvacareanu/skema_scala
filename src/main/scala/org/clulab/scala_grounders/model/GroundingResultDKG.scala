package org.clulab.scala_grounders.model

final case class GroundingResultDKG(
    score: Float,
    dkg: DKG,
    groundingDetails: GroundingDetails
)

/**
  * More details about the grounding
  *
  */
case class GroundingDetails(
  val grounderName: String,
  val matchingField: Option[String] = None,
  val matchingSlop: Option[Int] = None,
)
