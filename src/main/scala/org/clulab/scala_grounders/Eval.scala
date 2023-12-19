package org.clulab.scala_grounders

import upickle.default._
import scala.io.Source
import org.clulab.scala_grounders.model.DKG
import org.clulab.scala_grounders.grounding.SequentialGrounder
import org.clulab.scala_grounders.model.GroundingResultDKG

/**
  * (1) usually you want to return something. for future, adjust Neural Grounder threshold to favor recall
  * 
  * 
  * 
  * (i) What happens once you add the neural component
  * (ii) Curve as you vary the threshold for the neural component
  * (iii) Summary of this to the report
  *     - description of the numbers + numbers
  *     - what is different from last month (last month = neural one, this month = sieve)
  *     - discuss the sieve
  *     - discuss the results (emphasize that these results are orthogonal to the previous results, and we will work on the integration of both methods)
  * 
  */
object Eval extends App {

  // def 

  val grounder = SequentialGrounder()
  
  // Loading the data
  val evalLines = using(Source.fromFile("/home/rvacareanu/projects_7_2309/skema_scala/data/eval/prepared_grounding_extractions_documents_5Febbuckymodel_webdocs--COSMOS-data-grounding_extractions_documents_5Febbuckymodel_webdocs--COSMOS-data.jsonl")) { it =>
    it.getLines.map { line =>
      ujson.read(line)
    }.toIndexedSeq
  }

  val input = Seq("data/mira_dkg_epi_pretty.json")
  val data = input.flatMap { path => 
    using(Source.fromFile(path)) { it =>
      ujson.read(it.mkString).arr.map(it => DKG.fromJson(it))
    }
  }

  val idToData = data.map { dkg => (dkg.id, dkg) }.toMap

  val result = evalLines.map { it =>
    val examples = it("example").arr
    val text    = examples.head.apply("Text").str
    val role    = examples.head.apply("Role").str
    val context = examples.head.apply("Context").str

    val annotations = examples.map(it => (it("Candidate ID").str, it("Annotation").toString())).toMap


    val candidateDKGs = examples.map { it => idToData(it("Candidate ID").str) }.toSeq

    val groundings = grounder.ground(text, candidateDKGs, 5).force.toSeq
    (groundings, annotations)
  }

  println(result.count(_._1.isEmpty))
  System.exit(1)
  println(result.count(_._2.isEmpty))
  println(result.count(_._2.values.map { it => if (it == "0") 0 else 1 }.sum > 0))
  println(result.length)
  println(EvalMetrics.mrrOnlyExamplesWithRelevant(result))
  println(EvalMetrics.mrrWithNoDoc(result))
  println(EvalMetrics.mrrWithNoDoc(result, noDocMinMrrValue= (x) => 1/(x+1)))
  println("\n")
  println(EvalMetrics.meanAveragePrecisionAtKv2(result, k=1))
  println(EvalMetrics.meanAveragePrecisionAtKv2(result, k=2))
  println(EvalMetrics.meanAveragePrecisionAtKv2(result, k=3))
  println(EvalMetrics.meanAveragePrecisionAtKv2(result, k=4))
  println(EvalMetrics.meanAveragePrecisionAtKv2(result, k=5))

  // val ranks = result.zip(evalLines).flatMap { case (groundings, it) =>

  //   val examples = it("example").arr
  //   val annotations = examples.map(it => (it("Candidate ID").str, it("Annotation").toString())).toMap.mapValues(it => if (it=="2") "1" else it)
  //   val candidateIds = annotations.keys.toSeq
  //   val groundingScore = groundings.map(it => (it.dkg.id, "1")).toMap
  //   candidateIds.map { it =>
  //     (groundingScore.getOrElse(it, "0"), annotations(it))
  //   }
  // }
  // /**
  //  *  1 text you want to ground (t)
  //  *    
  //   *   10 grounding candidates that t will be grounded to (potentially)
  //   *   10 grounding candidates => 1 1 0 0 0 0 0 0 0 0  (annotations)
  //   *   grounder returns        => X _ X _ _ _ _ _ _ _
  //   *   grounder returns        => 1 0 1 0 0 0 0 0 0 0
  //   * 
  //   * 
  //   * P, R, F1 on the 1 class
  //   * (!) MRR -> Rank of your top correct candidate
  //   * Try RoBERTa-base (frozen) + linear layer for regression head
  //   */

  // println(evalLines.length)
  // // println(ranks)
  // println(ranks.count(it => it._1 == it._2)/ranks.length.toDouble)
  // println(ranks.count(it => it._1 == it._2))

  // println("-" * 20)
  // val p1 = result.zip(evalLines).map { case (groundings, it) =>

  //   val examples = it("example").arr
  //   val annotations = examples.map(it => (it("Candidate ID").str, it("Annotation").toString())).toMap.mapValues(it => if (it=="2") "1" else it).mapValues(_.toInt)
  //   val candidateIds = annotations.keys.toSeq
  //   val groundingScore = groundings.map(it => (it.dkg.id, "1")).toMap
  //   candidateIds.map { it =>
  //     (groundingScore.getOrElse(it, "0"), annotations(it))
  //   }.sortBy(-_._2)
  // }
  // println(p1.count(it => it.head._1 == it.head._2) / p1.length.toDouble)
  // println("-" * 20)


  // // Correlation score
  // val n = ranks.length
  // val x = ranks.map(_._1.toDouble)
  // val y = ranks.map(_._2.toDouble)

  // val sumX = x.sum
  // val sumY = y.sum
  // val sumXY = (x, y).zipped.map(_ * _).sum
  // val sumX2 = x.map(xi => xi * xi).sum
  // val sumY2 = y.map(yi => yi * yi).sum

  // val numerator = n * sumXY - sumX * sumY
  // val denominatorX = Math.sqrt(n * sumX2 - sumX * sumX)
  // val denominatorY = Math.sqrt(n * sumY2 - sumY * sumY)
  // println(numerator / (denominatorX * denominatorY))
  // println(result(0)._2.values.toIndexedSeq)
  // println(result(1)._2.values.toIndexedSeq)
  // println(result(2)._2.values.toIndexedSeq)
  // println(result(3)._2.values.toIndexedSeq)
  // println(result(4)._2.values.toIndexedSeq)
  // println(result.map(_.size))
  // println(result.map(_.size).sum)
  // println(evalLines.head)
  // println(evalLines.head.apply("example").arr.foreach(println))
  // println(data.head)
  // println(evalLines.head.apply("example").arr.map { it => it("Candidate ID").str }.map { it => idToData.contains(it) })
  // println(evalLines.map { it => it() })

}

/**
  * Particular to our dataset, there are cases where there are no relevant documents
  * There are multiple ways to handle this: 
  *   (1) Filter them out, keeping only documents where there is at least 1 relevant document
  *   (2) Assign a score based on the model's behavior:
  *     - If the model returned something, assign MinScore because 
  *       the relevant document (akin to a special NoDoc document) was not returned
  *     - If the model did not return anything, assign MaxScore because
  *       the relevant document (akin to a special NoDoc document) was returned the first position
  *     Note: Even here, instead of MinScore you can assign 1/(length+1), with the intuition being that
  *     the special NoDoc document comes right after all your returned documents
  * 
  */
object EvalMetrics {

  /**
    * Calculate MRR (Approach (1))
    * Keep only the datapoints where there are relevant documents
    * 
    */
  def mrrOnlyExamplesWithRelevant(results: Seq[(Seq[GroundingResultDKG], Map[String, String])]): Double = {
    val rrs = results.filter(it => it._2.values.exists(it => it == "1" || it == "2")).map { case (returnedDocs, goldDocs) =>
      val firstGood = returnedDocs.zipWithIndex
        .map { case (doc, id) => (doc, id + 1) } // Start from `1`, not `0`
        .first { case (doc, id) => goldDocs.getOrElse(doc.dkg.id, "0") == "1" || goldDocs.getOrElse(doc.dkg.id, "0") == "2" } 
        .map { case (doc, id) => 1.0/id }
        .getOrElse(0.0)

      firstGood
    }
    rrs.mean()
  }

  /**
    * Calculate MRR (Approach (2))
    * Assign a min value when there are no doc
    * 
    * @param results
    * @param noDocMinMrrValue -> A lambda function that returns the score when gold is NoDoc, but the model returned something, parameterized by model's length
    *                         The idea to parameterize it by model's length is to allow more flexibility. For example, if the model returned 4 documents, we can
    *                         (optimistically) interpret that NoDoc is on position 5
    * @param noDocMaxMrrValue -> A lambda function that returns the score when gold is NoDoc, and the model did not return anything
    * @return
    */
  def mrrWithNoDoc(
    results: Seq[(Seq[GroundingResultDKG], Map[String, String])], 
    noDocMinMrrValue: (Int) => Double = numberOfDocsReturned => 0.0, 
    noDocMaxMrrValue: () => Double = () => 1.0,
  ): Double = {
    val rrs = results.map { case (returnedDocs, goldDocs) =>
      val firstGood = returnedDocs.zipWithIndex
        .map { case (doc, id) => (doc, id + 1) } // Start from `1`, not `0`
        .first { case (doc, id) => goldDocs.getOrElse(doc.dkg.id, "0") == "1" || goldDocs.getOrElse(doc.dkg.id, "0") == "2" } 
        .map { case (doc, id) => 1.0/id }

      (firstGood.isEmpty, returnedDocs.isEmpty) match {
        case (false, false) => firstGood.get // If both are not empty, get the id of the first good
        case (false, true)  => noDocMinMrrValue(returnedDocs.length)
        case (true,  false) => 0.0
        case (true,  true)  => noDocMaxMrrValue()
      }
    }
    rrs.mean()
  }

  def precisionAtKOnlyExamplesWithRelevant(results: Seq[(Seq[GroundingResultDKG], Map[String, String])]): Double = {

    1.0
  }

  /**
    * 
    * Un-annotated returned documents are ignored
    *
    * @param results
    * @param k
    * @return Mean Average Precision@K
    */
  def meanAveragePrecisionAtKv1(results: Seq[(Seq[GroundingResultDKG], Map[String, String])], k: Int=1): Double = {
    val p = results.map { case (returnedDocs, goldDocs) =>
      val r = returnedDocs
        .filter(groundingResultDKG => goldDocs.contains(groundingResultDKG.dkg.id))
        .take(k)
        .map { groundingResultDKG =>
          goldDocs(groundingResultDKG.dkg.id)
        }
      (r.isEmpty, goldDocs.isEmpty) match {
        case (false, false) => r.count { result => (result == "1" || result == "2") }.toDouble/r.length
        case (false, true)  => 0.0
        case (true,  false) => 1.0
        case (true,  true)  => 1.0
      }
    }
    p.mean()
  }
  def meanAveragePrecisionAtKv12(results: Seq[(Seq[GroundingResultDKG], Map[String, String])], k: Int=1): Seq[Double] = {
    val p = results.map { case (returnedDocs, goldDocs) =>
      val r = returnedDocs
        .filter(groundingResultDKG => goldDocs.contains(groundingResultDKG.dkg.id))
        .take(k)
        .map { groundingResultDKG =>
          goldDocs(groundingResultDKG.dkg.id)
        }
      (r.isEmpty, goldDocs.isEmpty) match {
        case (false, false) => r.count { result => (result == "1" || result == "2") }.toDouble/r.length
        case (false, true)  => 0.0
        case (true,  false) => 1.0
        case (true,  true)  => 1.0
      }
    }
    p
  }

  /**
    * Un-annotated returned documents are considered incorrect
    *
    * @param results
    * @param k
    * @return Mean Average Precision@K
    */
  def meanAveragePrecisionAtKv2(results: Seq[(Seq[GroundingResultDKG], Map[String, String])], k: Int=1): Double = {
    val p = results.map { case (returnedDocs, goldDocs) =>
      val r = returnedDocs.take(k)
        .map { groundingResultDKG =>
          goldDocs.getOrElse(groundingResultDKG.dkg.id, "0")
        }
        
      (r.isEmpty, goldDocs.isEmpty) match {
        case (false, false) => r.count { result => (result == "1" || result == "2") }.toDouble/r.length
        case (false, true)  => 0.0
        case (true,  false) => 1.0
        case (true,  true)  => 1.0
      }
    }
    p.mean()
  }

}
