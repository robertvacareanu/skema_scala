package org.clulab.scala_grounders.grounding

import org.clulab.scala_grounders.model.{DKG, GroundingResultDKG, GroundingDetails}

import ai.onnxruntime.{OnnxTensor, OrtEnvironment, OrtSession}
import org.clulab.scala_transformers.encoder.Encoder

class NeuralGrounder(modelPath: String, threshold: Double) extends Grounder {

  override def getName: String = "Neural Grounder"

  def ground(text: String, groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    // groundingTargets.toStream.take(k).map { it => GroundingResultDKG(0, it, GroundingDetails(getName)) }
    ???
  }
  
}

object NGrounder extends App {
    val env = OrtEnvironment.getEnvironment()
    // val session = env.createSession("/home/rvacareanu/projects_7_2309/skema_python/results/2312/model_onnx/model.onnx", new OrtSession.SessionOptions())
    val encoder = Encoder.fromFile("/home/rvacareanu/projects_7_2309/skema_python/results/2312/model_onnx/model.onnx")

    println("Hello, World!")
    println(encoder.forward(Array(Array(1, 2, 3, 4, 5).map(_.longValue()), Array(1, 2, 3, 4, 5).map(_.longValue()))))
}
/**
 *  Integrate with
  * https://github.com/ml4ai/skema/blob/main/skema/text_reading/scala/src/main/scala/org/ml4ai/skema/text_reading/grounding/Grounder.scala
  * https://github.com/ml4ai/skema/blob/main/skema/text_reading/scala/src/main/scala/org/ml4ai/skema/text_reading/grounding/MiraEmbeddingsGrounder.scala#L167C7-L175
  */