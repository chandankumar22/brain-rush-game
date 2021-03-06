package com.ck.dev.tiptap.ui.games.rememberthecard

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.ck.dev.tiptap.R
import com.ck.dev.tiptap.extensions.changeStatusBarColor
import com.ck.dev.tiptap.extensions.handleGameAllLevelComplete
import com.ck.dev.tiptap.helpers.AppConstants
import com.ck.dev.tiptap.helpers.GameConstants.arrayOfImages
import com.ck.dev.tiptap.helpers.assetToBitmap
import com.ck.dev.tiptap.helpers.getRandomString
import com.ck.dev.tiptap.helpers.readJsonFromAsset
import com.ck.dev.tiptap.models.DialogData
import com.ck.dev.tiptap.models.RememberTheCardData
import com.ck.dev.tiptap.models.RememberTheCardGameRule
import com.ck.dev.tiptap.sounds.GameSound.playFlipCard
import com.ck.dev.tiptap.sounds.GameSound.playLevelFinish
import com.ck.dev.tiptap.sounds.GameSound.playLevelLose
import com.ck.dev.tiptap.sounds.GameSound.playRtcSuccess
import com.ck.dev.tiptap.sounds.GameSound.playRtcWrong
import com.ck.dev.tiptap.sounds.GameSound.playTimerSound
import com.ck.dev.tiptap.ui.custom.RememberCardView
import com.ck.dev.tiptap.ui.dialogs.ConfirmationDialog
import com.ck.dev.tiptap.ui.games.BaseFragment
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_find_the_num_game_play_screen.*
import kotlinx.android.synthetic.main.fragment_play_jumbled_words_game.*
import kotlinx.android.synthetic.main.fragment_play_remember_the_card_game.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import kotlin.random.Random

class PlayRememberTheCardGameFragment :
        BaseFragment(R.layout.fragment_play_remember_the_card_game) {

    private lateinit var _gameCompletePopup: ConfirmationDialog
    private var isGameCompletePopupToShow = false
    private lateinit var elementToAsk: RememberTheCardData
    private lateinit var gameName: String
    private var currentMoves: Int = 0
    private var cardVisibleTime: Int = 0
    private lateinit var rememberCardView: RememberCardView
    private lateinit var navController: NavController
    private lateinit var timer: CountDownTimer
    var isEndless: Boolean = false
    private var isExit: Boolean = false
    private var currentTimeLeft: Long = 0
    private var row: Int = 1
    private var col: Int = 1
    private var gameTimeLimit: Long = 2000
    private lateinit var currentLevel: String
    private val gameArgs: PlayRememberTheCardGameFragmentArgs by navArgs()
    private var isCardTimerShowing = true
    private var isAllPairsFormed = false
    private var model = ArrayList<RememberTheCardData>()
    private var timeSpentInEndless: Long = 0
    private var infTime = 9999999999L
    private var coins = 0
    private var player:MediaPlayer?=null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.i("onViewCreated called")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<ConstraintLayout>(R.id.header).visibility = View.GONE
        (requireActivity() as AppCompatActivity).changeStatusBarColor(R.color.primaryDarkColor)
        navController =
                Navigation.findNavController(requireActivity(), R.id.rem_the_card_nav_host_fragment)
        initGameParameters()
        initViews()
    }

    private fun initViews() {
        Timber.i("initViews called")
        rem_card_level_tv.text = getString(R.string.current_level, currentLevel)
        msg_tv.text =
                getString(R.string.you_have_s_seconds_to_remember_the_cards, cardVisibleTime.toString())
        val listOfRules: ArrayList<RememberTheCardData> = getRecyclerViewDataForCards()
        if (listOfRules.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "There was an issue while loading game. Please try again",
                    Toast.LENGTH_LONG
            ).show()
            navController.popBackStack()
        } else {
            rememberCardView = RememberCardView(requireContext(), listOfRules, col, this)
            game_container.addView(rememberCardView)
            setBackButtonHandling()
            if (isEndless) {
                setEndlessGameProperties()
            }
            startTimer((cardVisibleTime * 1000).toLong())
        }
    }

    fun checkIfMatchedTb(isMatched: Boolean = false, isCardFlipped: Boolean = false) {
        lifecycleScope.launch {
            when {
                isMatched -> {
                    requireContext().playRtcSuccess()
                }
                isCardFlipped -> {
                    requireContext().playFlipCard()
                }
                else -> {
                    requireContext().playRtcWrong()
                }
            }

        }

    }

    fun checkIfMatched(rememberTheCardData: RememberTheCardData, position: Int) {
        Timber.i("checkIfMatched called")
        currentMoves++
        live_moves_tv.text = currentMoves.toString()
        if (rememberTheCardData.id == elementToAsk.id) {
            lifecycleScope.launch {
                requireContext().playRtcSuccess()
            }
            model.remove(rememberTheCardData)
            rememberCardView.showCardAt(position)
            setEndlessGameProperties()
        } else {
            lifecycleScope.launch {
                requireContext().playRtcWrong()
            }
            rememberCardView.hideCardAt(position)
            Toast.makeText(requireContext(), "Wrong card picked", Toast.LENGTH_LONG).show()
        }
    }

    private fun setEndlessGameProperties() {
        Timber.i("setEndlessGameProperties called")
        if (model.size == 0) {
            showGameCompletePopup()
            return
        }
        elementToAsk =
                if (model.size == 1) {
                    model[0]
                } else {
                    val random = Random.nextInt(0, model.size)
                    model[random]
                }
        Glide.with(requireContext()).load(elementToAsk.drawableRes).into(where_card)
    }

    private fun initGameParameters() {
        Timber.i("initGameParameters called")
        isEndless = gameArgs.isEndless
        row = gameArgs.row
        col = gameArgs.col
        gameTimeLimit = gameArgs.timeLimit
        currentLevel = gameArgs.level
        cardVisibleTime = gameArgs.cardVisibleTime
        gameName = gameArgs.gameName
        coins = gameArgs.coins
    }

    private fun getRecyclerViewDataForCards(): ArrayList<RememberTheCardData> {
        Timber.i("getDrawablesForCard called")
        val list = ArrayList<RememberTheCardData>()
        val listOfDrawable = getTheRandomImagesList()
        Timber.i("size of drawables of filtered list ${listOfDrawable.size}")
        if (isEndless) {
            if (listOfDrawable.size != (row * col)) {
                return arrayListOf()
            }
            for (i in 0 until (row * col)) {
                list.add(RememberTheCardData(listOfDrawable[i], getRandomString()))
            }
            val toReturn = list.shuffled() as ArrayList<RememberTheCardData>
            toReturn.forEach {
                model.add(it)
            }
            return toReturn
        }
        if (listOfDrawable.size != (row * col) / 2) {
            return arrayListOf()
        }
        for (i in 0 until (row * col) / 2) {
            list.add(RememberTheCardData(listOfDrawable[i], getRandomString()))
        }
        val doubleList = ArrayList<RememberTheCardData>()
        list.forEach {
            doubleList.add(
                    RememberTheCardData(
                            it.drawableRes,
                            it.id,
                            it.isRevealed,
                            it.isLocked,
                            true
                    )
            )
        }
        val double = list + doubleList
        val toReturn = double.shuffled() as ArrayList<RememberTheCardData>
        toReturn.forEach {
            model.add(it)
        }
        return toReturn
    }

    private fun startTimer(gameTimeLimit: Long) {
        Timber.i("startTimer called")
        timer = object : CountDownTimer(gameTimeLimit, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //Timber.i("ticking at ${gameTimeLimit / 1000}")
                currentTimeLeft = millisUntilFinished
                if (isEndless) {
                    if (isCardTimerShowing) {
                        if (millisUntilFinished in 4000L..4999L || millisUntilFinished in 1000L..2000L) {
                            lifecycleScope.launch {
                                player = requireContext().playTimerSound()
                            }
                        }
                        rem_card_timer_tv.text =
                                getString(R.string.time_left, (currentTimeLeft / 1000).toString())
                    } else {
                        timeSpentInEndless = infTime - millisUntilFinished
                        rem_card_timer_tv.text =
                                getString(R.string.time_left, (timeSpentInEndless / 1000).toString())
                    }
                } else {
                    if (millisUntilFinished in 4000L..4999L || millisUntilFinished in 1000L..2000L) {
                        lifecycleScope.launch {
                            requireContext().playTimerSound()
                        }
                    }
                    rem_card_timer_tv.text =
                            getString(R.string.time_left, (currentTimeLeft / 1000).toString())
                }
            }

            override fun onFinish() {
                Timber.i("timer onFinish called")
                player?.let {
                    if(it.isPlaying)   it.stop()
                    it.release()
                }
                if (isCardTimerShowing) {
                    rememberCardView.hideAllCards()
                    isCardTimerShowing = false
                    if (!isEndless) {
                        msg_tv.text =
                                "You have ${this@PlayRememberTheCardGameFragment.gameTimeLimit} seconds to find the pair of similar cards."
                        startTimer(this@PlayRememberTheCardGameFragment.gameTimeLimit * 1000)
                    } else {
                        where_card_cont.visibility = View.VISIBLE
                        msg_tv.visibility = View.INVISIBLE
                        Glide.with(requireContext()).load(elementToAsk.drawableRes).into(where_card)
                        startTimer(infTime - timeSpentInEndless)
                    }
                } else {
                    if (!isEndless) {
                        showGameCompletePopup()
                    } else {
                        showGameCompletePopup()
                        /*where_card_cont.visibility = View.VISIBLE
                        msg_tv.visibility = View.GONE
                        Glide.with(requireContext()).load(elementToAsk.drawableRes).into(where_card)*/
                        //startTimer(infTime - timeSpentInEndless)
                    }
                }

            }
        }
        timer.start()
    }

    fun increaseMovesCounter() {
        Timber.i("increaseMovesCounter called")
        currentMoves++
        live_moves_tv.text = currentMoves.toString()
    }

    fun updateTime(isGameFinished: Boolean = false) {
        Timber.i("updateTime called")
        if (!isGameFinished) {
            Timber.i("current time spent at ${currentTimeLeft / 1000}")
            timer.cancel()
            currentTimeLeft += 5000
            Timber.i("after increasing by 5 seconds ${currentTimeLeft / 1000}")
            startTimer(currentTimeLeft)
        } else {
            isAllPairsFormed = true
            showGameCompletePopup()
        }
    }

    private fun showGameCompletePopup() {
        Timber.i("showGameCompletePopup called")
        lifecycleScope.launch {
            viewModel.apply {
                if(isEndless){
                    updateHighScoreIfApplicable(
                            gameName,
                            currentLevel,
                            currentMoves, isLowScoreToSave = true, coinsToAdd = coins
                    )
                }
                else if(isAllPairsFormed) {
                    updateHighScoreIfApplicable(
                            gameName,
                            currentLevel,
                            currentMoves, isLowScoreToSave = true, coinsToAdd = coins
                    )
                }
                updateGameLevel(
                        gameName,
                        (currentLevel.toInt() + 1).toString()
                )
                updateTotalGamePlayed(gameName)
                updateTotalTimePlayed(gameName, if (isEndless) timeSpentInEndless / 1000 else currentTimeLeft / 1000)
            }
            if (!isEndless && !isAllPairsFormed) {
                requireContext().playLevelLose()
            } else {
                requireContext().playLevelFinish()
            }
        }
        val dialogData = if (isEndless) DialogData(
                title = getString(R.string.rem_the_card_game_complete_title),
                content = getString(
                        R.string.rem_the_card_game_complete_content,
                        currentMoves.toString()
                ),
                posBtnText = getString(R.string.game_complete_positive_btn_txt),
                negBtnText = getString(R.string.game_exit_positive_btn_txt),
                posListener = {
                    setNextLevel()
                },
                megListener = {
                    exitGame()
                }
        ) else {
            val title: String
            val content: String
            val posBtnText: String
            val negBtnText: String
            val posListener: () -> Unit
            val negListener: () -> Unit
            if (isAllPairsFormed) {
                title = getString(R.string.rem_the_card_game_complete_title)
                content = getString(
                        R.string.rem_the_card_game_complete_content,
                        currentMoves.toString()
                )
                posBtnText = getString(R.string.game_complete_positive_btn_txt)
                negBtnText = getString(R.string.game_exit_positive_btn_txt)
                posListener = {
                    if (::navController.isInitialized) {
                        navController.navigate(R.id.action_playRememberTheCardGameFragment_self)
                        setNextLevel()
                    }
                }
                negListener = { exitGame() }
            } else {
                title = getString(R.string.game_complete_title)
                content = "Your time is up and all pairs are not found. Try again ?"
                posBtnText = getString(R.string.game_retry_btn_txt)
                negBtnText = getString(R.string.game_exit_positive_btn_txt)
                posListener = { retryGame() }
                negListener = {
                    if (::navController.isInitialized) {
                        exitGame()
                    }
                }
            }
            DialogData(
                    title = title,
                    content = content,
                    posBtnText = negBtnText,
                    negBtnText = posBtnText,
                    posListener = negListener,
                    megListener = posListener
            )
        }
        timer.cancel()
        _gameCompletePopup = ConfirmationDialog.newInstance(dialogData)
        _gameCompletePopup.isCancelable = false
        if (lifecycle.currentState == Lifecycle.State.RESUMED) {
            _gameCompletePopup.show(parentFragmentManager, AppConstants.GAME_COMPLETE_TAG)
        } else {
            isGameCompletePopupToShow = true
        }
    }

    private fun retryGame() {
        Timber.i("retryGame called")
        val action =
                PlayRememberTheCardGameFragmentDirections.actionPlayRememberTheCardGameFragmentSelf(
                        row = row,
                        col = col,
                        timeLimit = gameTimeLimit,
                        level = currentLevel, isEndless = false,
                        cardVisibleTime = cardVisibleTime,
                        gameName = gameName, coins = coins
                )
        navController.navigate(action)
    }

    private fun setBackButtonHandling() {
        Timber.i("setBackButtonHandling called")
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val dialogData = DialogData(
                    title = getString(R.string.game_exit_title),
                    content = getString(R.string.game_exit_content),
                    posBtnText = getString(R.string.game_exit_positive_btn_txt),
                    negBtnText = getString(R.string.game_exit_negative_btn_txt),
                    posListener = {
                        if (::navController.isInitialized) {
                            isExit = true
                            exitGame()
                        }
                    },
                    megListener = {
                        if (isEndless) {
                            if (!isCardTimerShowing) {
                                startTimer(infTime - timeSpentInEndless)
                            }
                        } else startTimer(currentTimeLeft)
                    }
            )
            if (!isCardTimerShowing) {
                timer.cancel()
                val instance = ConfirmationDialog.newInstance(dialogData)
                instance.isCancelable = false
                instance.show(parentFragmentManager, AppConstants.GAME_EXIT_TAG)
            }
        }
    }

    private fun setNextLevel() {
        Timber.i("setNextLevel called")
        val rulesJson = (requireActivity() as AppCompatActivity).readJsonFromAsset(
                AppConstants.REMEMBER_CARDS_GAME_RULE_FILE_NAME
        )
        val json = JSONObject(rulesJson)
        val obj = if (isEndless) {
            Gson().fromJson(json.getJSONArray("endless").toString(), Array<RememberTheCardGameRule>::class.java)
        } else {
            Gson().fromJson(json.getJSONArray("time-bound").toString(), Array<RememberTheCardGameRule>::class.java)
        }
        timer.cancel()
        if (currentLevel.toInt() == obj.size) {
            requireContext().handleGameAllLevelComplete()
            exitGame()
        } else {
            val gameRule = obj[currentLevel.toInt()]
            gameTimeLimit = 0L
            val action =
                    PlayRememberTheCardGameFragmentDirections.actionPlayRememberTheCardGameFragmentSelf(
                            row = gameRule.row,
                            col = gameRule.col,
                            timeLimit = gameRule.timeLimit,
                            level = gameRule.level, isEndless = isEndless,
                            cardVisibleTime = cardVisibleTime,
                            gameName = gameName, coins = coins
                    )
            navController.navigate(action)
        }
    }

    private fun exitGame(it: DialogFragment? = null) {
        Timber.i("exitGame called")
        it?.dismiss()
        val action = PlayRememberTheCardGameFragmentDirections.actionPlayRememberTheCardGameFragmentToRememberTheCardGameLevelsFragment(gameName)
        navController.navigate(action)
    }

    private fun getTheRandomImagesList(): ArrayList<Bitmap> {
        Timber.i("getTheRandomImagesList called")
        val listOfDrawables = ArrayList<Bitmap>()
        val imagesNameList = (arrayOfImages.shuffled() as ArrayList)
        if (isEndless) {
            for (i in 0 until (row * col)) {
                listOfDrawables.apply {
                    with(requireContext()) {
                        assetToBitmap(imagesNameList[i])?.let { add(it) }
                    }
                }
            }
            return listOfDrawables.shuffled() as ArrayList<Bitmap>
        }
        for (i in 0 until (row * col) / 2) {
            listOfDrawables.apply {
                with(requireContext()) {
                    assetToBitmap(imagesNameList[i])?.let { add(it) }
                }
            }
        }
        return listOfDrawables.shuffled() as ArrayList<Bitmap>
    }

    override fun onResume() {
        super.onResume()
        if (isGameCompletePopupToShow) {
            isGameCompletePopupToShow = false
            _gameCompletePopup.show(parentFragmentManager, AppConstants.GAME_COMPLETE_TAG)
        }
    }
}