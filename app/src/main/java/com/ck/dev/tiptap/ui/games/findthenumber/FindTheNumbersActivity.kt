package com.ck.dev.tiptap.ui.games.findthenumber

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ck.dev.tiptap.R
import com.ck.dev.tiptap.extensions.fetchColor
import com.ck.dev.tiptap.extensions.fetchDrawable
import com.ck.dev.tiptap.ui.GameMainScreen
import kotlinx.android.synthetic.main.activity_find_the_numbers.*
import kotlinx.android.synthetic.main.activity_game_main_screen.*

class FindTheNumbersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_the_numbers)
        find_the_num_header.setBackgroundColor(fetchColor(R.color.primaryLightColor))
    }

}