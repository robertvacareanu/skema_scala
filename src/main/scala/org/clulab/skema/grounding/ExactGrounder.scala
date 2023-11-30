package org.clulab.skema.grounding

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
import org.clulab.skema.{EnhancedType, EnhancedCollections}
import org.clulab.skema.model.DKG
import org.clulab.skema.model.GroundingResultDKG
import org.clulab.skema.indexing.BuildIndex
import org.clulab.skema.model.GroundingDetails


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


  def exactMatch(fieldName: String, phrase: String): Query = {
    new TermQuery(new Term("em_" + fieldName, "^ " + phrase.toLowerCase + " $"))
  }

}
