package org.clulab.scala_grounders

import org.apache.lucene.search.Query
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery

object Sieve {
  
  /**
    * This returns an exact match Query
    * The text in `fieldName` should be equal (`==`) to the text in `phrase`
    * Example: 
    *   - (1)
    *     - Text in `fieldName` is "John Doe"
    *     - Text in `phrase` is "John"
    *     - Match? Answer: No
    *   - (2)
    *     - Text in `fieldName` is "John Doe"
    *     - Text in `phrase` is "John Doe"
    *     - Match? Answer: Yes
    *
    * @param fieldName (String) -> The fieldName that this query will be on
    * @param phrase    (String) -> What to search for
    * @return
    */
  def exactMatch(fieldName: String, phrase: String): Query = {
    new TermQuery(new Term("em_" + fieldName, "^ " + phrase + " $"))
  }

  def fuzzyMatch(fieldName: String, phrase: String, slop: Int = 0): Query = {
    new PhraseQuery.Builder().let { it =>
      // TODO Use tokenizer
      phrase.split(" ").foreach { word =>
        it.add(new Term(fieldName, word))        
      }

      it.setSlop(slop)
      it.build() 
    }
  }


}
