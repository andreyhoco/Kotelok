package com.designdrivendevelopment.kotelok.screens.trainers.writer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.designdrivendevelopment.kotelok.R
import com.designdrivendevelopment.kotelok.application.KotelokApplication
import com.designdrivendevelopment.kotelok.entities.Dictionary
import com.designdrivendevelopment.kotelok.screens.trainers.ScoreState
import com.designdrivendevelopment.kotelok.screens.trainers.trainersUtils.ViewAlphaAnimation
import com.designdrivendevelopment.kotelok.trainer.TrainerWriter
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers

@Suppress("TooManyFunctions")
class TrainWriteFragment : Fragment() {
    private val viewModel: TrainWriteViewModel by viewModels {
        TrainWriteViewModelFactory(
            TrainerWriter(
                (requireActivity().application as KotelokApplication).appComponent.writerLearnDefRepository,
                (requireActivity().application as KotelokApplication).appComponent.changeStatisticsRepositoryImpl
            ),
            Dispatchers.IO
        )
    }
    private var inputText: TextInputLayout? = null
    private var flashcard: LinearLayout? = null
    private var wordWriting: TextView? = null
    private var wordExample: TextView? = null
    private var checkButton: Button? = null
    private var nextWordButton: Button? = null
    private var answerCheckResultText: TextView? = null
    private var trainingReportLayout: ConstraintLayout? = null
    private var backToDictionariesButton: TextView? = null
    private var continueTrainingBtn: TextView? = null
    private var reportText: TextView? = null
    private var inputGroup: LinearLayout? = null
    private var dictionaryId = Dictionary.DEFAULT_DICT_ID
    private val alphaAnimation = ViewAlphaAnimation()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dictionaryId = arguments?.getLong(ID_KEY_BUNDLE) ?: Dictionary.DEFAULT_DICT_ID
        if (savedInstanceState == null) {
            viewModel.loadDict(dictionaryId, onlyUnlearned = true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_train_write, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.writer_trainer_title)
        layoutInflater.inflate(R.layout.layout_trainer_result_report, view as ViewGroup)
        initViews(view)
        trainingReportLayout?.isVisible = false
        setupListeners()

        viewModel.currentWord.observe(this, this::setWord)
        viewModel.answerState.observe(this) { state ->
            onStateChanged(state)
        }
        viewModel.scoreState.observe(this, this::handleScoreState)
    }

    override fun onResume() {
        super.onResume()
        alphaAnimation.endFadeAnimations()
        alphaAnimation.endAppearanceAnimations()
    }

    override fun onStop() {
        super.onStop()
        alphaAnimation.endFadeAnimations()
        alphaAnimation.endAppearanceAnimations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearViews()
    }

    private fun setupListeners() {
        checkButton?.setOnClickListener {
            inputText?.error = null
            val answer = inputText?.editText?.text?.toString().orEmpty()
            if (answer.isEmpty()) {
                inputText?.error = getString(R.string.error_empty_writing)
                return@setOnClickListener
            }
            viewModel.onUserAnswer(answer)
        }
        nextWordButton?.setOnClickListener { viewModel.onNext() }
        backToDictionariesButton?.setOnClickListener { requireActivity().onBackPressed() }
        continueTrainingBtn?.setOnClickListener { viewModel.repeat(dictionaryId) }
    }

    private fun handleScoreState(state: ScoreState) {
        when (state) {
            is ScoreState.Hide -> {
                trainingReportLayout?.isVisible = false
                alphaAnimation.startAppearance(flashcard, SCORE_APPEARANCE_DURATION shr 1)
                alphaAnimation.startAppearance(inputGroup, SCORE_APPEARANCE_DURATION shr 1)
            }

            is ScoreState.Show -> {
                alphaAnimation.startFade(flashcard, SCORE_APPEARANCE_DURATION shr 1)
                alphaAnimation.startFade(inputGroup, SCORE_APPEARANCE_DURATION shr 1) {
                    reportText?.text = state.text
                    alphaAnimation.startAppearance(trainingReportLayout, SCORE_APPEARANCE_DURATION shr 1)
                }
            }
        }
    }

    private fun setWord(word: Word) {
        wordWriting?.text = word.writingTr
        wordExample?.text = word.exampleTr
    }

    private fun onStateChanged(state: AnswerState) {
        when (state) {
            is AnswerState.NotAnswered -> {
                inputText?.editText?.text?.clear()
                inputText?.isEnabled = true
                alphaAnimation.startFade(answerCheckResultText, ANSWER_TEXT_ANIM_DURATION)
                changeCheckButtonState(true)
                changeNextWordButtonState(false)
            }

            is AnswerState.Answered -> {
                changeCheckButtonState(false)
                changeNextWordButtonState(true)
                alphaAnimation.startAppearance(answerCheckResultText, ANSWER_TEXT_ANIM_DURATION)
                inputText?.isEnabled = false

                if (state is AnswerState.Answered.Right) {
                    answerCheckResultText?.text = getString(R.string.answer_right_word, state.writing)
                } else {
                    answerCheckResultText?.text = getString(R.string.answer_wrong_word, state.writing)
                }
            }
        }
    }

    private fun changeCheckButtonState(enabled: Boolean) {
        checkButton?.isEnabled = enabled
        checkButton?.isVisible = enabled
    }

    private fun changeNextWordButtonState(enabled: Boolean) {
        nextWordButton?.isEnabled = enabled
        nextWordButton?.isVisible = enabled
    }

    private fun initViews(view: View) {
        inputText = view.findViewById(R.id.input_word)
        flashcard = view.findViewById(R.id.flashcard)
        wordWriting = view.findViewById(R.id.word_writing_ru)
        wordExample = view.findViewById(R.id.word_example_ru)
        checkButton = view.findViewById(R.id.check_button)
        nextWordButton = view.findViewById(R.id.next_word_button)
        backToDictionariesButton = view.findViewById(R.id.back_button)
        continueTrainingBtn = view.findViewById(R.id.repeat_button)
        trainingReportLayout = view.findViewById(R.id.layout_report_frame)
        reportText = view.findViewById(R.id.text_after_complete)
        answerCheckResultText = view.findViewById(R.id.text_check_result)
        inputGroup = view.findViewById(R.id.input_group)
    }

    private fun clearViews() {
        inputText = null
        flashcard = null
        wordWriting = null
        wordExample = null
        checkButton = null
        nextWordButton = null
        backToDictionariesButton = null
        continueTrainingBtn = null
        trainingReportLayout = null
        reportText = null
        answerCheckResultText = null
        inputGroup = null
    }

    companion object {
        private const val ID_KEY_BUNDLE = "id"
        private const val SCORE_APPEARANCE_DURATION = 500L
        private const val ANSWER_TEXT_ANIM_DURATION = 200L

        fun newInstance(dictionaryId: Long) = TrainWriteFragment().apply {
            arguments = Bundle().apply {
                putLong(ID_KEY_BUNDLE, dictionaryId)
            }
        }
    }
}
