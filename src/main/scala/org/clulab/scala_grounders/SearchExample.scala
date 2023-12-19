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

object SearchExample extends App {
  val index  = FSDirectory.open(new File("index/").toPath)
  val reader = DirectoryReader.open(index)
  val is     = new IndexSearcher(reader)
  // val qp     = new QueryParser("text", new StandardAnalyzer())
  // val query  = qp.parse("intentional entity")
  val searchText = "disease transmission model intentional entity"
  // val query = new PhraseQuery.Builder().let { it =>
  //   searchText.split(" ").foreach { word => 
  //     it.add(new Term("name", word))
  //   }
  //   it.build()
  // }
  // val result = is.search(query, Int.MaxValue).scoreDocs
  // println(is.search(query, Int.MaxValue).totalHits)
  // System.exit(1)
  val ngrams = Seq(3, 2, 1).flatMap { ngram => searchText.split(" ").sliding(ngram).map { it => it.mkString(" ")}}

  val result = Seq(
    ("name",        3, 0),
    ("synonym1",    3, 0),
    ("synonym2",    3, 0),
    ("synonym3",    3, 0),
    ("synonym4",    3, 0),
    ("synonym5",    3, 0),
    ("name",        2, 0),
    ("synonym1",    2, 0),
    ("synonym2",    2, 0),
    ("synonym3",    2, 0),
    ("synonym4",    2, 0),
    ("synonym5",    2, 0),
    ("name",        1, 0),
    ("synonym1",    1, 0),
    ("synonym2",    1, 0),
    ("synonym3",    1, 0),
    ("synonym4",    1, 0),
    ("synonym5",    1, 0),
    ("description", 3, 0),
    ("description", 2, 0),
    ("text",        3, 0),
    ("name",        3, 1),
    ("synonym1",    3, 1),
    ("synonym2",    3, 1),
    ("synonym3",    3, 1),
    ("synonym4",    3, 1),
    ("synonym5",    3, 1),
    ("name",        2, 2),
    ("synonym1",    2, 2),
    ("synonym2",    2, 2),
    ("synonym3",    2, 2),
    ("synonym4",    2, 2),
    ("synonym5",    2, 2),

  ).flatMap { case (fieldName, ngramSize, editDistance) => ngramSearch(is, ngramString = searchText, fieldName, ngramSize = ngramSize, editDistance = editDistance)
   .map     { case (td, ngram) => (td, ngram, ngramSize, fieldName) } }
   .map     { case (td, ngram, ngramSize, fieldName) => (td.scoreDocs.toSeq, ngram, ngramSize, fieldName) }
   .first(_._1.length > 0)

  println(result.headOption.getOrElse(""))
  println(result.headOption.map { it => is.doc(it._1.head.doc) })

  def ngramSearch(index: IndexSearcher, ngramString: String, fieldName: String, ngramSize: Int, editDistance: Int) = {
    editDistance match {
      case 0 => ngramExactSearch(index=index, ngramString=ngramString, fieldName=fieldName, ngramSize=ngramSize)
      case _ => ngramFuzzySearch(index=index, ngramString=ngramString, fieldName=fieldName, ngramSize=ngramSize, editDistance=editDistance)
    }
  }

  def ngramExactSearch(index: IndexSearcher, ngramString: String, fieldName: String, ngramSize: Int) = {
    val ngrams = searchText.split(" ").sliding(ngramSize).map { it => it.mkString(" ")}

    ngrams.map { ngram => 
      val words = ngram.split(" ")
      val query = new PhraseQuery.Builder().let { it =>
        words.foreach { word => 
          it.add(new Term(fieldName, word))
        }
        it.build()
      }
      // val result = is.search(query, Int.MaxValue).scoreDocs
      val result = (is.search(query, Int.MaxValue), ngram)
      println(result._1.scoreDocs.length, fieldName, ngram)
      result
    }
 
  }

  def ngramFuzzySearch(index: IndexSearcher, ngramString: String, fieldName: String, ngramSize: Int, editDistance: Int) = {
    val ngrams = searchText.split(" ").sliding(ngramSize).map { it => it.mkString(" ")}

    ngrams.map { ngram => 
      val words = ngram.split(" ")
      val query = new BooleanQuery.Builder().let { it =>
        words.foreach { word => 
          it.add(new BooleanClause(new FuzzyQuery(new Term(fieldName, word), editDistance), BooleanClause.Occur.MUST))
        }
        it.build()
      }
      // val result = is.search(query, Int.MaxValue).scoreDocs
      val result = (is.search(query, Int.MaxValue), ngram)
      println(result._1.scoreDocs.length, fieldName, ngram, editDistance)
      result
    }
 
  }

}
