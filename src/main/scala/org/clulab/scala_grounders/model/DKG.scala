package org.clulab.scala_grounders.model

import ujson.Value

final case class DKG(
  id: String,
  name: String,
  description: Option[String],
  synonyms: Seq[DKGSynonym],
) 

object DKG {
  def fromJson(json: Value): DKG = {
    val jsonDict = json.obj
    DKG(
      id=jsonDict("id").str,
      name=jsonDict("name").str,
      description=jsonDict.get("description").map(_.str),
      synonyms=jsonDict("synonyms").arr.map(DKGSynonym.fromJson).toSeq,
    )
  }
}

final case class DKGSynonym(
  value: String,
  synonymType: Option[String],
)

object DKGSynonym {
  def fromJson(json: Value): DKGSynonym = {
    val jsonDict = json.obj
    DKGSynonym(
      value=jsonDict("value").str,
      synonymType=jsonDict.get("type").map(_.str),
    )
  }
}