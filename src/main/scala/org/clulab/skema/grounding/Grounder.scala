package org.clulab.skema.grounding

import com.typesafe.config.Config
import org.clulab.skema.model.DKG
import com.typesafe.config.ConfigFactory

import collection.JavaConverters._
import org.clulab.skema.model.GroundingResultDKG

import org.clulab.skema.{using, first}

import scala.io.Source
import java.util.ArrayList


trait Grounder {
  /**
    * A human-readable way of identifying the grounders
    *
    * @return This grounder's name
    */
  def getName: String
  def ground(
    text: String,
    groundingTargets: Seq[DKG],
    k: Int = 1
  ): Stream[GroundingResultDKG]
}
object Grounder {
  private val nameToGrounder = Map(
    // "exact_match"    -> ExactGrounder,
    // "fuzzy_matcher"  -> FuzzyGrounder,
    // "neural_matcher" -> NeuralGrounder,
  )

  /**
    * Build a `Grounder` from a `Config`
    *
    * @param config -> The config, containing the necessary information to build a grounder
    * @return       -> A grounder
    */
  def mkGrounder(config: Config): Grounder = {
    config.getString("type") match {
      case "exact_matcher"  => new ExactGrounder(config.getList("fieldNames").asScala.toSeq.map { it => it.unwrapped().asInstanceOf[ArrayList[String]].asScala.toSeq })
      case "fuzzy_matcher"  => new FuzzyGrounder(config.getList("fieldNames").asScala.toSeq.map { it => it.unwrapped().asInstanceOf[ArrayList[String]].asScala.toSeq }, config.getIntList("slops").asScala.map(_.toInt))
      case "neural_matcher" => new NeuralGrounder(config.getString("modelPath"), config.getDouble("threshold"))
      case it@_ => throw new IllegalArgumentException(f"Unrecognized grounding type (${it})")
    }
  }
}

class SequentialGrounder extends Grounder {
  lazy val components = getComponents()
  def getName = "Sequential Grounder"

  def ground(text: String, groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    components.toStream.flatMap { grounder =>
      grounder.ground(text, groundingTargets, k)
    }.take(k)
  }

  /**
    * A constructor function that creates the components for this Sequential Grounder
    * It looks over the configuration file for the `compoent$i` field inside `sieve`, and
    * constructs each component accordingto its definition
    *
    * @return A Seq[Grounder]
    */
  def getComponents(): Seq[Grounder] = {
    val config = ConfigFactory.load().getConfig("grounder")
    val k = config.getInt("behavior.sieve.k")
    // Create all the given components
    // Use stream because we do not know beforehand how many there are 
    // But there has to be a finite number of them (otherwise they could not have been written to file)
    val components = Stream.from(1) 
          .takeWhile(it => config.hasPath(f"behavior.sieve.component${it}")) // Stop once there are no more
          .map { componentNumber => // Create Grounder
            val componentConfig = config.getConfig(f"behavior.sieve.component${componentNumber}")
            Grounder.mkGrounder(componentConfig)
          }.force.toSeq // Stop the lazy behavior here
    
    components
  }

}
