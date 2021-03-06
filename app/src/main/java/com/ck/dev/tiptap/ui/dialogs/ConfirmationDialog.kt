package com.ck.dev.tiptap.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.ck.dev.tiptap.R
import com.ck.dev.tiptap.models.DialogData
import com.ck.dev.tiptap.sounds.GameSound.playBtnClickSound
import kotlinx.android.synthetic.main.confirmation_dialog.*
import kotlinx.coroutines.launch
import java.util.*

class ConfirmationDialog : DialogFragment() {


    companion object {
        fun newInstance(dialogData: DialogData?): ConfirmationDialog {
            val dialog = ConfirmationDialog()
            val args = Bundle()
            if (dialogData != null) {
                args.putParcelable("dialogData", dialogData)
            }
            dialog.arguments = args
            return dialog
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Objects.requireNonNull(dialog)?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.confirmation_dialog, null, false)
    }

    //dialog view is ready
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params: ViewGroup.LayoutParams = it.attributes
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = params as WindowManager.LayoutParams
        }

        val dialogData: DialogData? = arguments?.getParcelable("dialogData")
        dialogData?.let {
            dialog_title_tv.text = it.title
            dialog_content_tv.text = it.content
            dialog_positive_button.apply {
                if(it.posBtnText.trim().isEmpty()) dialog_positive_button.visibility = View.GONE
                setBtnText(it.posBtnText)
                setOnClickListener { view ->
                    lifecycleScope.launch {
                        requireContext().playBtnClickSound()
                    }
                    it.posListener()
                    dismiss()
                }
            }
            dialog_negative_button.apply {
                setBtnText(it.negBtnText)
                setOnClickListener { view ->
                    lifecycleScope.launch {
                        requireContext().playBtnClickSound()
                    }
                    it.megListener()
                    dismiss()
                }
            }
            if(it.coinsToTake>0){
                /*get_coins_tv.visibility = View.VISIBLE
                coins_for_extra_container.visibility = View.VISIBLE
                get_coins_tv.text = it.extraCoinsText
                coins_for_extra.text = it.coinsToTake.toString()
                get_coins_tv.setOnClickListener {_->
                    dismiss()
                    it.extraCoinsListener()
                }*/
            }
        }
    }
}