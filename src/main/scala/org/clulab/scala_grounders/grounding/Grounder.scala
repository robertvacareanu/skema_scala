package org.clulab.scala_grounders.grounding

import com.typesafe.config.Config
import org.clulab.scala_grounders.model.DKG
import com.typesafe.config.ConfigFactory

import collection.JavaConverters._
import org.clulab.scala_grounders.model.GroundingResultDKG

import org.clulab.scala_grounders.{using, first}

import scala.io.Source
import java.util.ArrayList


/**
  * A generic `Grounder` trait which, minimally, forces the implementations to do:
  *   - define a name for the grounder 
  *   - implement `ground`, a generic method which has 3 parameters:
  *     - (1) what we want to ground
  *     - (2) where we want to ground it (i.e. `grounding candidates` or `grounding targets`)
  *     - (3) how many results to return
  * 
  * This trait does not force any particular style of implementation. Some potential implementations are:
  *   - stateless, which does not do any caching/etc
  *   - a more efficient version where the class implementing the trait receives the `groundingTargets` as
  *     a pararameter at construction time, then caches is (i.e. for an exact matcher, we can build the index once; same 
  *     for fuzzy matcher)
  * 
  */
trait Grounder {
  /**
    * A human-readable way of identifying the grounders
    *
    * @return This grounder's name
    */
  def getName: String

  /**
    * 
    *
    * @param text: String               -> The text to be grounded onto `groundingTargets`
    * @param groundingTargets: Seq[DKG] -> The candidates on which we wish to ground `text`
    * @param k: Int                     -> How many results to return
    * @return
    */
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

/**
  * A `SequentialGrounder` is an implementation of 
  * `Grounder` that has a sequence of grounders which
  * are applied sequentially
  * 
  * This sequential application allows us to implicitly declare
  * priorities
  *
  */
class SequentialGrounder extends Grounder {
  /**
    * The components of this grounder
    * It is lazy to delay the construction until first utilization
    *
    * @return
    */
  lazy val components = getComponents()

  /**
    * Name of this grounder
    *
    * @return
    */
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

object SequentialGrounder {
  def apply() = new SequentialGrounder()
}

object GrounderApp extends App {

  example2()

  def getGrounder(): Seq[Grounder] = {
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
    
    return components
  }

  def example1() = {
    val input = Seq("data/mira_dkg_epi_pretty_small2.json")
    val data = input.flatMap { path => 
      using(Source.fromFile(path)) { it =>
        ujson.read(it.mkString).arr.map(it => DKG.fromJson(it))
      }
    }

    val config = ConfigFactory.load().getConfig("grounder")
    println(config)
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
    println(components)
    // Run over for result; Call `toStream` to only run grounders until we get `k` results
    val result = components.toStream.flatMap { grounder =>
      grounder.ground("autoimmune hemolytic anaemia", data, k)
    }.take(k).force.toList
    result.foreach(println)
  }

  def example2() = {
    val input = Seq("data/mira_dkg_epi_pretty_small2.json")
    val data = input.flatMap { path => 
      using(Source.fromFile(path)) { it =>
        ujson.read(it.mkString).arr.map(it => DKG.fromJson(it))
      }
    }

    val components = new SequentialGrounder()
    println(components)
    // Run over for result; Call `toStream` to only run grounders until we get `k` results
    val result = components.ground("autoimmune hemolytic anaemia", data, 5).toList
    result.foreach(println)
  }

}
