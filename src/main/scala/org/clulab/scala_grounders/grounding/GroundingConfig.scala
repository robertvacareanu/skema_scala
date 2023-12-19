package org.clulab.scala_grounders.grounding

import com.typesafe.config.Config

import collection.JavaConverters._

/**
  * How the grounder should look like
  *
  * @param cache -> Where to cache
  * @param cacheDir
  * @param fieldNames
  * @param k
  */
final case class GroundingConfig(
    cache: String,
    cacheDir: String,
    fieldNames: Array[String],
    k: Int,
)

object GroundingConfig {
    def fromConfig(config: Config): GroundingConfig = {
        GroundingConfig(
            cache = config.getString(""),
            cacheDir = config.getString(""),
            fieldNames = config.getStringList("").asScala.toArray,
            k = config.getInt(""),
        )
    }
}
