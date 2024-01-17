package org.clulab.scala_grounders.grounding

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
import org.clulab.scala_grounders.{EnhancedType, EnhancedCollections}
import org.clulab.scala_grounders.model.DKG
import org.clulab.scala_grounders.model.GroundingResultDKG
import org.clulab.scala_grounders.indexing.BuildIndex
import org.clulab.scala_grounders.model.GroundingDetails

/**
  * This grounder attempts to do "exact matching"
  * As an example, assuming we search for "dog", and we have: 
  *   - Doc1({"field_a": "dog", "field_b": "dog cat"})
  *   - Doc2({"field_a": "dog cat", "field_b": "cat dog"})
  * 
  * Assuming we search on `field_a` and `field_b`, then Doc1 is a match (because of `field_a`, dog == dog)
  * Doc2, however, is not an exact match (dog is not == with dog cat, dog is not == with cat dog)
  * 
  * So, in other words, this grounder does an "=="
  *
  * @param fieldGroups: Seq[Seq[String]] -> Each string (say, if we flatten this), is a field in the doc
  *                                         They are grouped (i.e. we have Seq[Seq[String]] instead of simply Seq[String])
  *                                         because of priorities; We first attempt all the field names available in the first
  *                                         element, then second, etc, as much as needed
  */
class ExactGrounder(fieldGroups: Seq[Seq[String]]) extends Grounder {

  override def getName: String = "Exact Grounder"

  override def ground(text: String, groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    val is: IndexSearcher = new IndexSearcher(DirectoryReader.open(BuildIndex.buildIndexFromDocs(groundingTargets, inMemory=true)))
    val targets = groundingTargets.map(it => it.id -> it).toMap

    val fieldNames = fieldGroups.toStream.flatMap { it => it.toStream }

    
    val result = fieldGroups.toStream.flatMap { fieldNames =>
      fieldNames.toStream.flatMap { fieldName => 
        // Search over the given fieldName
        val td = is.search(exactMatch(fieldName, text), k)
        // Map all the hits to the desired result type (lazily)
        td.scoreDocs.map { sd => 
          val id    = is.doc(sd.doc).getField("id").stringValue
          val score = sd.score 
          GroundingResultDKG(score, targets(id), GroundingDetails(getName, matchingField=Some(fieldName)))
        }
      }.sortBy(-_.score)
    }

    result
  }

  def mkFast(is: IndexSearcher): FastExactGrounder = new FastExactGrounder(fieldGroups, is)

  def exactMatch(fieldName: String, phrase: String): Query = {
    new TermQuery(new Term("em_" + fieldName, "^ " + phrase.toLowerCase + " $"))
  }

}
object Test extends App {
  val a = Stream.from(1)
  println(a)
}