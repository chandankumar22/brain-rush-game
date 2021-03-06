package com.ck.dev.tiptap.ui.games.jumbledwords

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.ck.dev.tiptap.R
import com.ck.dev.tiptap.adapters.JumbledWordsLevelsAdapter
import com.ck.dev.tiptap.data.entity.Games
import com.ck.dev.tiptap.extensions.changeStatusBarColor
import com.ck.dev.tiptap.extensions.setHeaderBgColor
import com.ck.dev.tiptap.helpers.AppConstants.JUMBLED_WORDS_GAME_RULE_FILE_NAME
import com.ck.dev.tiptap.helpers.ClickSound
import com.ck.dev.tiptap.helpers.GameConstants.ENDLESS
import com.ck.dev.tiptap.helpers.GameConstants.JUMBLED_NUMBER_GAME_NAME_ENDLESS
import com.ck.dev.tiptap.helpers.GameConstants.JUMBLED_NUMBER_GAME_NAME_TIME_BOUND
import com.ck.dev.tiptap.helpers.GameConstants.TIME_BOUND
import com.ck.dev.tiptap.helpers.readJsonFromAsset
import com.ck.dev.tiptap.models.JumbledWordGameLevelData
import com.ck.dev.tiptap.sounds.GameSound.playBtnClickSound
import com.ck.dev.tiptap.ui.games.BaseFragment
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_game_levels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

class JumbledWordsLevelsFragment : BaseFragment(R.layout.fragment_game_levels), ClickSound {

    private lateinit var navController: NavController

    private val gameArgs: JumbledWordsLevelsFragmentArgs by navArgs()
    private var mode = ENDLESS
    private var gameData: Games = Games(JUMBLED_NUMBER_GAME_NAME_ENDLESS)
    private var gameName = JUMBLED_NUMBER_GAME_NAME_ENDLESS

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated called")
        super.onViewCreated(view, savedInstanceState)
        mode = gameArgs.gameMode
        gameName =
                if (mode == ENDLESS) JUMBLED_NUMBER_GAME_NAME_ENDLESS else if (mode == TIME_BOUND) JUMBLED_NUMBER_GAME_NAME_TIME_BOUND else ""
        navController =
                Navigation.findNavController(requireActivity(), R.id.jumbled_words_nav_host_fragment)
        setLevelsRecyclerView()
        (requireActivity() as AppCompatActivity).setHeaderBgColor(R.color.primaryLightColor)
        requireActivity().findViewById<ConstraintLayout>(R.id.header).visibility = View.VISIBLE
        (requireActivity() as AppCompatActivity).changeStatusBarColor(R.color.primaryLightColor)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
    }

    private fun setLevelsRecyclerView() {
        Timber.i("setLevelsRecyclerView called")
        lifecycleScope.launch {
            viewModel.insertGameIfNotAdded(gameName)
            val completedGameLevels = viewModel.getCompletedLevels(gameName)
            if (!completedGameLevels.isNullOrEmpty()) {
                val sortedLevels = completedGameLevels.sortedByDescending {
                    it.currentLevel.toInt()
                }
                gameData = Games(
                        sortedLevels[0].gameName,
                        sortedLevels[0].currentLevel
                )
            }
            withContext(Dispatchers.Main) {
                val rulesJson =
                        (requireActivity() as AppCompatActivity).readJsonFromAsset(
                                JUMBLED_WORDS_GAME_RULE_FILE_NAME
                        )

                val json = JSONObject(rulesJson)
                val easyGameRules = json.getJSONArray(mode).toString()
                val jumbledRules = Gson().fromJson(easyGameRules, Array<JumbledWordGameLevelData>::class.java)
                val getLevels = viewModel.getJumbledWordGameRules(gameData, jumbledRules)
                game_levels_tv.text = "${gameData?.currentLevel}/${getLevels.size}"
                val gridLayoutManager = GridLayoutManager(requireContext(), 5)
                levels_rv.layoutManager = gridLayoutManager
                levels_rv.adapter = JumbledWordsLevelsAdapter(getLevels, navController, gameName,this@JumbledWordsLevelsFragment)
            }
        }
    }

    override fun playSound() {
        lifecycleScope.launch {
            requireContext().playBtnClickSound()
        }
    }
}