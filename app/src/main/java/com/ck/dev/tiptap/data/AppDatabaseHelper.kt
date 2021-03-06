package com.ck.dev.tiptap.data

import com.ck.dev.tiptap.data.entity.BestScores
import com.ck.dev.tiptap.data.entity.Games

interface AppDatabaseHelper {
    suspend fun getGameDataByName(gameName:String): Games?
    suspend fun getCompletedLevels(gameName:String): List<Games>
    suspend fun insertGame(gameName:Games)
    suspend fun updateGameLevel(gameName: String,currentLevel:String)
    suspend fun getHighScore(gameName: String,levelNum:String):Int?
    suspend fun insertBestScore(bestScores: BestScores)
    suspend fun getHighScoreForAllLevels(gameName: String):List<BestScores>
    suspend fun updateBestScore(bestScores: BestScores)
    suspend fun updateLongestPlayedForInfiniteGame(gameName: String,gridSize:Int,longestPlayed:Long)
    suspend fun updateBestScoreForInfiniteGame(gameName: String,gridSize:Int,score:Int)
    suspend fun getHighScoreForInfinite(gameName: String,gridSize:Int):BestScores
    suspend fun updateTotalTimePlayed(gameName: String, totalTime: Long)
    suspend fun updateTotalGamePlayed(gameName: String)
}