package org.clulab.skema.grounding

import org.clulab.skema.model.{DKG, GroundingResultDKG, GroundingDetails}


class NeuralGrounder(modelPath: String, threshold: Double) extends Grounder {

  override def getName: String = "Neural Grounder"

  def ground(text: String, groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    // groundingTargets.toStream.take(k).map { it => GroundingResultDKG(0, it, GroundingDetails(getName)) }
    ???
  }
  
}
