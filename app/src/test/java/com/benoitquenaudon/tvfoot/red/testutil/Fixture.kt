package com.benoitquenaudon.tvfoot.red.testutil

import com.benoitquenaudon.tvfoot.red.app.data.entity.Match
import com.benoitquenaudon.tvfoot.red.app.data.entity.Tag
import com.benoitquenaudon.tvfoot.red.app.data.entity.Team
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.source
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Inject

class Fixture @Inject constructor(private val moshi: Moshi) {

  private val listMatchType = Types.newParameterizedType(List::class.java, Match::class.java)
  private val listTagType = Types.newParameterizedType(List::class.java, Tag::class.java)
  private val listTeamType = Types.newParameterizedType(List::class.java, Team::class.java)

  fun anyMatches(): List<Match> {
    return anyObject("matches_sample.json", listMatchType)
  }

  fun matchesListA(): List<Match> {
    return anyObject("matches_list_a.json", listMatchType)
  }

  fun matchesListB(): List<Match> {
    return anyObject("matches_list_b.json", listMatchType)
  }

  fun anyMatchesShort(): List<Match> {
    return anyObject("matches_sample_short.json", listMatchType)
  }

  fun anyMatch(): Match {
    return anyObject("match_sample.json", Match::class.java)
  }

  fun anyNextMatches(): List<Match> {
    return anyObject("matches_next_sample.json", listMatchType)
  }

  fun anyTeams(): List<Team> {
    return anyObject("teams_sample.json", listTeamType)
  }

  private fun <T> anyObject(filename: String, typeOfT: Type): T {
    val inputStream = this.javaClass.classLoader.getResourceAsStream(filename)

    try {
      val reader = JsonReader.of(inputStream.source().buffer())
      val adapter = moshi.adapter<T>(typeOfT)
      val result = adapter.fromJson(reader)

      reader.close()

      return result ?: throw IllegalStateException("no data for $filename and $typeOfT")
    } catch (e: IOException) {
      throw RuntimeException("fixture $filename failed", e)
    }
  }

  fun tags(): List<Tag> {
    return anyObject("tags.json", listTagType)
  }
}
