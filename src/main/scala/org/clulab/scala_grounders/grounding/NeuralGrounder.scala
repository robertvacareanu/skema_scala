package org.clulab.scala_grounders.grounding

import org.clulab.scala_grounders.model.{DKG, GroundingResultDKG, GroundingDetails}

import ai.onnxruntime.{OnnxTensor, OrtEnvironment, OrtSession}
import org.clulab.scala_transformers.tokenizer.Tokenizer
import org.clulab.scala_transformers.tokenizer.jni.ScalaJniTokenizer
import java.util.{HashMap => JHashMap}
import org.apache.lucene.search.IndexSearcher
import java.io.DataInputStream

class NeuralGrounder(modelPath: String, threshold: Double) extends Grounder {

  private val nn = new UnderlyingNeuralNetworkImplementation(modelPath)
  
  override def getName: String = "Neural Grounder"

  def ground(text: String, context: Option[String], groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    // groundingTargets.toStream.take(k).map { it => GroundingResultDKG(0, it, GroundingDetails(getName)) }
    groundingTargets.map { dkg =>
      val score = nn.forwardSingle(text + context.map(it => "\n" + it).getOrElse(""), dkg.name)
      GroundingResultDKG(
        score=score,
        dkg=dkg,
        groundingDetails = GroundingDetails(getName, Some("name"), None)
      )
    }.sortBy(-_.score).filter(_.score > threshold).toStream
  }

  /**
    * This class does a very specific type of forward on very specific models.
    * Concretely, it needs an onnx model path to a model trained for grounding, 
    * in the form of taking two strings, concatenated.
    * For example: "<string1> [SPECIAL SEP TOKEN] <string2>" (e.g., like BERT 
    * next sentence prediction)
    * Needs special care based on the model; For example, BERT and BERT-derivatives 
    * add a special token ([CLS]) at the beginning, then [SEP] to separate between 
    * the two sequences; Other might add [CLS] at the end, etc.
    * This implementation does not handle this.
    *
    * @param onnxModelPath: String -> Path to an onnx model saved to disk
    */
  private class UnderlyingNeuralNetworkImplementation(onnxModelPath: String) {

    
    protected def ortSessionFromResource(resourceName: String): OrtSession = {
      val connection = getClass.getResource(resourceName).openConnection
      val contentLength = connection.getContentLength
      val bytes = new Array[Byte](contentLength)
      val inputStream = getClass.getResourceAsStream(resourceName)
      val dataInputStream = new DataInputStream(inputStream)

      try {
        dataInputStream.readFully(bytes)
      }
      finally {
        dataInputStream.close()
      }
      env.createSession(bytes, new OrtSession.SessionOptions)
    }

    private val env       = OrtEnvironment.getEnvironment
    private val session   = ortSessionFromResource(onnxModelPath)
    private val tokenizer = ScalaJniTokenizer("microsoft/deberta-v3-base", addPrefixSpace=false)

    def forwardSingle(text1: String, text2: String): Float = {
      val inputIds1 = tokenizer.tokenize(text1.split(" ")).tokenIds
      // FIXME maybe not all models add [CLS] at the beginning
      val inputIds2 = tokenizer.tokenize(text2.split(" ")).tokenIds.tail // `.tail` ensures that we do not capture the `[CLS]` token that is being added to all sequences
      val inputIds  = Array.concat(inputIds1, inputIds2)
      val attentionMask = Array.fill(inputIds1.length + inputIds2.length)(1l)
      val tokenTypeIds  = Array.concat(Array.fill(inputIds1.length)(0l), Array.fill(inputIds2.length)(1l))
      val inputs = new JHashMap[String, OnnxTensor]()
      inputs.put("input_ids",      OnnxTensor.createTensor(env, Array(inputIds.map(_.toLong))))
      inputs.put("attention_mask", OnnxTensor.createTensor(env, Array(attentionMask.map(_.toLong))))
      // inputs.put("token_type_ids", OnnxTensor.createTensor(env, Array(tokenTypeIds))) // For DeBERTa it looks like `token_type_ids` are not used; Same result given when all `0`, when `correct` and when 1 item is 4 (when one item is 4 we should have seen an error)
      // val encoderOutput = encoderSession.run(inputs).get(0).getValue.asInstanceOf[Array[Array[Array[Float]]]]
      val result = session.run(inputs).get(0).getValue().asInstanceOf[Array[Array[Float]]].head
      // println(result.toSeq)
      // The result is an array of length 2, corresponding to logit for class 0 (not grounded) and 
      // logit for class 1 (grounded)
      val lastSoftmaxed = Math.exp(result.last) / (Math.exp(result.head) + Math.exp(result.last))
      lastSoftmaxed.toFloat
    }
  }

  def mkFast(is: IndexSearcher): Grounder = this
  
}


object NGrounder extends App {
  
    val grounder = new NeuralGrounder("/org/clulab/grounders/model.onnx", 0.5)
    val result = grounder.ground(
      text="exposed",
      context=Some("exposed (E)"),
      groundingTargets = Seq(
        DKG("id1", "exposed individuals", None, Seq.empty),
        DKG("id2", "exposed contacts", None, Seq.empty),
        DKG("id3", "number of cases exposed by local unknown exposure", None, Seq.empty),
        DKG("id4", "crocodile", None, Seq.empty),
      )
    )
    result.foreach(println)
}
