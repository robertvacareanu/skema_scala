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
  * This grounder attempts to do "fuzzy matching" with slops (i.e., gaps)
  * As an example, assuming we search for "dog cat", and we have: 
  *   - Doc1({"field_a": "dog dog", "field_b": "dog dog cat"})
  *   - Doc2({"field_a": "dog cat dog", "field_b": "cat dog"})
  * 
  * Assuming we search on `field_a` and `field_b`, then Doc1 is a match (because of `field_b`),
  * and Doc2 is a match (because `field_a`)
  * This grounder allows for slops (i.e. gaps in between the search phrase)
  * 
  *
  * @param fieldGroups: Seq[Seq[String]] -> Each string (say, if we flatten this), is a field in the doc
  *                                         They are grouped (i.e. we have Seq[Seq[String]] instead of simply Seq[String])
  *                                         because of priorities; We first attempt all the field names available in the first
  *                                         element, then second, etc, as much as needed
  * @param slops:       Seq[Int]         -> The gaps that are allowed when seaching; 
  *                                         For example, a slop=0 means no gap (`dog cat` for `dog cat),
  *                                         a slop=1 means a gap of 1 item (`dog _ cat` for `dog cat`)
  *                                         a slop=2 means a gap of 2 items (`dog _ _ cat` for `dog cat`), etc
  */
class FuzzySlopGrounder(fieldGroups: Seq[Seq[String]], slops: Seq[Int]) extends Grounder {

  override def getName: String = "Fuzzy Slop Grounder"


  override def ground(text: String, context: Option[String], groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    val is: IndexSearcher = new IndexSearcher(DirectoryReader.open(BuildIndex.buildIndexFromDocs(groundingTargets, inMemory=true)))
    val targets = groundingTargets.map(it => it.id -> it).toMap

    val fields     = fieldGroups.toStream.map(_.toStream)
    val slopValues = slops.toStream

    val result = slopValues.flatMap { slop => 
      fields.flatMap { fieldNames => 
        fieldNames.flatMap { fieldName => 
          // Search over the given fieldName and the given slop
          val td = is.search(buildQuery(fieldName, text, slop), Int.MaxValue)
          // Map all the hits to the desired result type (lazily)
          td.scoreDocs.map { sd => 
            val id    = is.doc(sd.doc).getField("id").stringValue
            val score = sd.score 
            GroundingResultDKG(score, targets(id), GroundingDetails(getName, matchingField=Some(fieldName), matchingSlop=Some(slop)))
          }.toSeq 
        }.sortBy(-_.score) 
      }
    }

    result
  }

  def buildQuery(fieldName: String, phrase: String, slop: Int = 0): Query = {
    /**
      * test: dog dog
      * candidate: dog dog cat (matches)
      * candidate: dog cat dog (matches)
      * candidate: dog cat (does not matches)
      * 
      */
    new PhraseQuery.Builder().let { it =>
      // TODO Use tokenizer
      phrase.split(" ").foreach { word =>
        it.add(new Term(fieldName, word))        
      }

      it.setSlop(slop)
      it.build() 
    }
  }

  def mkFast(is: IndexSearcher): FastFuzzySlopGrounder = new FastFuzzySlopGrounder(fieldGroups, slops, is)

}

/**
  * This grounder is a "Fast" implementation of the "FuzzyGrounder" (please see @ref org.clulab.scala_grounders.grounding.FuzzyGrounder)
  * It is "Fast" because we do not reindex upon every call to "ground"
  *
  * @param fieldGroups: Seq[Seq[String]] -> Each string (say, if we flatten this), is a field in the doc
  *                                         They are grouped (i.e. we have Seq[Seq[String]] instead of simply Seq[String])
  *                                         because of priorities; We first attempt all the field names available in the first
  *                                         element, then second, etc, as much as needed
  * @param slops:       Seq[Int]         -> The gaps that are allowed when seaching; 
  *                                         For example, a slop=0 means no gap (`dog cat` for `dog cat),
  *                                         a slop=1 means a gap of 1 item (`dog _ cat` for `dog cat`)
  *                                         a slop=2 means a gap of 2 items (`dog _ _ cat` for `dog cat`), etc
  * @param is: IndexSearcher             -> The Index Searcher to use
  */
class FastFuzzySlopGrounder(fieldGroups: Seq[Seq[String]], slops: Seq[Int], is: IndexSearcher) extends FuzzySlopGrounder(fieldGroups, slops) {

  override def ground(text: String, context: Option[String], groundingTargets: Seq[DKG], k: Int): Stream[GroundingResultDKG] = {
    val targets = groundingTargets.map(it => it.id -> it).toMap

    val fields     = fieldGroups.toStream.map(_.toStream)
    val slopValues = slops.toStream

    val result = slopValues.flatMap { slop => 
      fields.flatMap { fieldNames => 
        fieldNames.flatMap { fieldName => 
          // Search over the given fieldName and the given slop
          val td = is.search(buildQuery(fieldName, text, slop), Int.MaxValue)
          // Map all the hits to the desired result type (lazily)
          td.scoreDocs.map { sd => 
            val id    = is.doc(sd.doc).getField("id").stringValue
            val score = sd.score 
            GroundingResultDKG(score, targets(id), GroundingDetails(getName, matchingField=Some(fieldName), matchingSlop=Some(slop)))
          }.toSeq 
        }.sortBy(-_.score) 
      }
    }

    result
  }

}
