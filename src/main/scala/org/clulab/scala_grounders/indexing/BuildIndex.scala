package org.clulab.scala_grounders.indexing

import java.io.File
import java.util.Calendar

import com.typesafe.config.ConfigFactory
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.document.{Document, Field, TextField}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory
// import org.clulab.dynet.Utils
// import org.clulab.processors.clu.CluProcessor
import org.clulab.scala_grounders.{using, first}

import scala.io.Source
import org.clulab.scala_grounders.model.DKG
import org.apache.lucene.analysis.ngram.NGramTokenizer
import org.apache.lucene.document.StringField
import org.apache.lucene.store.RAMDirectory
import org.apache.lucene.store.MMapDirectory
import org.apache.lucene.store.BaseDirectory

object BuildIndex extends App {

  buildIndexFromPaths(Seq("data/mira_dkg_epi_pretty.json"), false)

  def buildIndexFromDocs(input: Seq[DKG], inMemory: Boolean): BaseDirectory = {
    val indexPath: String = "index/"
    lazy val analyzer = new WhitespaceAnalyzer
    lazy val index    = inMemory match {
      case false => FSDirectory.open(new File(indexPath).toPath)
      case true  => new RAMDirectory
    }

    val config = new IndexWriterConfig(analyzer)
    val iw     = new IndexWriter(index, config)
    
    
    // println(f"${Calendar.getInstance().getTime}")
    input.foreach { d =>
      addDoc(iw, d)
    }
    // println(f"${Calendar.getInstance().getTime}")
    iw.close()
    if (!inMemory) {
      index.close()
    }
    analyzer.close()

    index
  }

  def buildIndexFromPaths(input: Seq[String], inMemory: Boolean): BaseDirectory = {    
    println(f"${Calendar.getInstance().getTime}")
    val data = input.flatMap { path => 
      using(Source.fromFile(path)) { it =>
        ujson.read(it.mkString).arr.map(it => DKG.fromJson(it))
      }
    }
    buildIndexFromDocs(data, inMemory)
  }


  def addDoc(w: IndexWriter, dkg: DKG): Unit = {
    val doc = new Document()
    doc.add(new TextField("id",   dkg.id,   Field.Store.YES))
    doc.add(new TextField("name", dkg.name, Field.Store.YES))
    dkg.description.foreach { description => doc.add(new TextField("description", dkg.description.getOrElse(""), Field.Store.YES))}
    doc.add(new TextField("description", dkg.description.getOrElse(""), Field.Store.YES))
    dkg.synonyms.filter(_.synonymType.getOrElse("").contains("hasExactSynonym")).zipWithIndex.foreach { case (synonym, idx) =>
      doc.add(new TextField(f"synonym${idx}", synonym.value, Field.Store.YES))
    }
    val text = Seq(dkg.name, dkg.description.getOrElse(""), dkg.synonyms.filter(_.synonymType.getOrElse("").contains("hasExactSynonym")).map(_.value).mkString(" ")).mkString(" ")
    doc.add(new TextField("text", text, Field.Store.YES))
    val ngrams = Seq(1, 2, 3).flatMap { ngram => text.split(" ").sliding(ngram).mkString(" ")}.mkString(" ")

    doc.add(new TextField("text_ngrams", ngrams, Field.Store.NO))

    // Exact Matching
    // The idea of adding "^" and "$" came from https://blogs.perl.org/users/mark_leighton_fisher/2012/01/stupid-lucene-tricks-exact-match-starts-with-ends-with.html
    // TODO Maybe change?
    doc.add(new StringField("em_name", "^ " + dkg.name + " $", Field.Store.YES))
    dkg.synonyms.filter(_.synonymType.getOrElse("").contains("hasExactSynonym")).zipWithIndex.foreach { case (synonym, idx) =>
      doc.add(new StringField(f"em_synonym${idx+1}", "^ " + synonym.value + " $", Field.Store.YES))
    }

    w.addDocument(doc)
  }
}



/*
Another Grounding App (maybe for inspiration) -> https://github.com/ml4ai/skema/blob/main/skema/text_reading/scala/src/main/scala/org/ml4ai/skema/text_reading/grounding/PipelineGrounder.scala
[x] Keep text field
[x] Index For synonyms, have individual synonyms (hyperparam for how many to add (e.g. 1-10))
[x] (1) Sieve Match (first -> second -> etc): (name -> synonym1 -> .. -> synonym10 -> description -> text) (NGram Match)
[x] (2) Fuzzy Match (edit distance = 1) over  (name -> synonym1 -> .. -> synonym10) (~ operator in Lucene; you can include an edit distance -- https://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
[] (3) Neural Matcher (maybe with FAISS? (index will be pretty small))
*/

/*

Drop NGram
Sieve order: (1) -> (2) -> (3)
[x] (1) Sieve Match (first -> second -> etc): (name -> synonym1 -> .. -> synonym10) (Exact, === there)
[x] (2) Sieve Match (first -> second -> etc): (name -> synonym1 -> .. -> synonym10 -> description -> text) (Exact and/or Fuzzy; Allow for inclusions (does not have to be ===))
[] (3) Neural Matcher
[] (4) Back off to GloVe emeddings if (3)'s score is somewhat low
  - GloVe embeddings at sub-word level (FastText?)
*/