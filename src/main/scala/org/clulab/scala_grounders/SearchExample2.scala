package org.clulab.scala_grounders

import com.typesafe.config.ConfigFactory
import org.apache.lucene.index.{DirectoryReader, IndexWriterConfig, Term}
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.{BooleanQuery, IndexSearcher, Query, TermQuery}
import org.apache.lucene.store.FSDirectory
import java.io.File
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.analysis.core.WhitespaceTokenizer
import org.apache.lucene.document.Document

/**
 * How should grounding work.
 * 
 * You pass a list of JSON files (e.g. `mira_dgk_epi_pretty.json`)
 * 
 * You use the grounder on a sequence of terms,
 * waiting to get back
 * 
 * - Return top K (ranked)
 * - Information of the type of match that happend:
 *     - the type of sieve + Sieve details (sieve specific) -> (return original JSON Doc, score, sieve type + details)
 * 
 * Sieve Configs
 *   - config file 
 *  
*/


object SearchExample2 extends App {
  val index  = FSDirectory.open(new File("index/").toPath)
  val reader = DirectoryReader.open(index)
  val is     = new IndexSearcher(reader)
  // val qp     = new QueryParser("text", new StandardAnalyzer())
  // val query  = qp.parse("intentional entity")
  // val searchText = "reproduction pathogen"
  val searchText = "mixed acidophil-basophil adenoma"

  // val result = Seq("name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10").toStream.map { it =>
  //   is.search(Sieve.exactMatch(it, searchText), Int.MaxValue)
  // }.first { it =>
  //   it.scoreDocs.length > 0
  // }

  // println(result)
  // println(result.map(_.totalHits))
  // println(result.map(_.scoreDocs.length))
  // println(is.search(Sieve.exactMatch("name", searchText), Int.MaxValue).scoreDocs.length)
  // println(is.search(Sieve.fuzzyMatch("description", "contamination acquisition"), Int.MaxValue).scoreDocs.length)
  // println(is.search(Sieve.fuzzyMatch("description", "contamination acquisition", 1), Int.MaxValue).scoreDocs.length)
  // println(is.search(Sieve.fuzzyMatch("description", "contamination acquisition", 2), Int.MaxValue).scoreDocs.length)
  // println(is.search(Sieve.fuzzyMatch("description", "contamination acquisition", 3), Int.MaxValue).scoreDocs.length)
  // println(is.search(Sieve.fuzzyMatch("description", "contamination acquisition", 4000), Int.MaxValue).scoreDocs.length)
  val result = sieveGrounding(searchText, is, 10).take(1).map { doc => doc.getField("id").stringValue }
  println("-"*20)
  result.foreach(println)
  println("-"*20)


  def sieveGrounding(searchText: String, is: IndexSearcher, maxHits: Int = Int.MaxValue): Seq[Document] = {
    val firstSieve = Stream("name", "synonym1", "synonym2", "synonym3", "synonym4", "synonym5", "synonym6", "synonym7", "synonym8", "synonym9", "synonym10").toStream

    val fsr = firstSieve.map { it => is.search(Sieve.exactMatch(it, searchText), maxHits) }.first { it =>
      it.scoreDocs.length > 0
    }.map { td => td.scoreDocs.map { sd => is.doc(sd.doc) }.toSeq }
    if (fsr.isDefined) {
      return fsr.get
    }

    val secondSieve = firstSieve :+ "description"

    val slops = Stream(1, 2, 4, 8)

    val ssr = slops.flatMap { slop =>
      secondSieve.map { fieldName =>
        is.search(Sieve.fuzzyMatch(fieldName, searchText, slop), Int.MaxValue)
      }
    }
    .first(_.scoreDocs.length > 0)
    .map { td => td.scoreDocs.map { sd => is.doc(sd.doc) }.toSeq }

    if (ssr.isDefined) {
      return ssr.get
    }

    return Seq.empty

  }


}
