package com.designdrivendevelopment.kotelok.screens.trainers.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.designdrivendevelopment.kotelok.R
import com.designdrivendevelopment.kotelok.application.KotelokApplication
import com.designdrivendevelopment.kotelok.entities.Dictionary
import com.designdrivendevelopment.kotelok.screens.trainers.ScoreState
import com.designdrivendevelopment.kotelok.screens.trainers.swipe.SwipeLayoutManager
import com.designdrivendevelopment.kotelok.screens.trainers.trainersUtils.ViewAlphaAnimation
import com.designdrivendevelopment.kotelok.trainer.TrainerCards

@Suppress("TooManyFunctions")
class TrainFlashcardsFragment : Fragment() {
    private val viewModel: TrainFlashcardsViewModel by viewModels {
        TrainFlashcardsViewModelFactory(
            TrainerCards(
                (requireActivity().application as KotelokApplication).appComponent.cardsLearnDefRepository,
                (requireActivity().application as KotelokApplication).appComponent.changeStatisticsRepositoryImpl,
            ),
            CARDS_ON_SCREEN
        )
    }

    private var dictionaryId = Dictionary.DEFAULT_DICT_ID
    private var cardsStack: RecyclerView? = null
    private var trainingReportLayout: ConstraintLayout? = null
    private var reportText: TextView? = null
    private var backToDictionariesBtn: TextView? = null
    private var continueTrainingBtn: TextView? = null
    private val flashCardAnimations = FlashCardAnimations()
    private val scoreAnimation = ViewAlphaAnimation()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dictionaryId = arguments?.getLong(ID_KEY_BUNDLE) ?: Dictionary.DEFAULT_DICT_ID
        if (savedInstanceState == null) {
            viewModel.loadDict(dictionaryId, onlyUnlearned = true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_train_flashcards, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        activity.title = getString(R.string.cards_trainer_title)

        val topPos = savedInstanceState?.getInt(POS_KEY_BUNDLE) ?: START_POS
        layoutInflater.inflate(R.layout.layout_trainer_result_report, view as ViewGroup)

        initViews(view)
        trainingReportLayout?.isVisible = false
        setupClickListeners()

        val adapter = CardsAdapter(requireContext(), flashCardAnimations)
        val lm = SwipeLayoutManager.build {
            doOnSwipeLeft { pos -> viewModel.onUserAnswer(pos, answer = true) }
            doOnSwipeRight { pos -> viewModel.onUserAnswer(pos, answer = false) }
            setPivotYType(SwipeLayoutManager.Config.Pivot.CENTER)
            setRelativeStackHeight(RELATIVE_HEIGHT)
            saveTopOnItemChanges(true)
        }
        cardsStack?.adapter = adapter
        cardsStack?.layoutManager = lm
        cardsStack?.itemAnimator = object : DefaultItemAnimator() {
            override fun animateMove(
                holder: RecyclerView.ViewHolder?,
                fromX: Int,
                fromY: Int,
                toX: Int,
                toY: Int
            ): Boolean {
                return if (fromY == toY) super.animateMove(holder, fromX, fromY, toX, toY) else false
            }
        }
        cardsStack?.scrollToPosition(topPos)

        viewModel.definitionsCards.observe(this) { cards ->
            adapter.submitList(cards)
            if (cards.isEmpty()) cardsStack?.scrollToPosition(START_POS)
        }
        viewModel.scoreState.observe(this, this::handleScoreState)
    }

    override fun onStop() {
        super.onStop()
        flashCardAnimations.endAnimations()
        scoreAnimation.endAppearanceAnimations()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val topPos = (cardsStack?.layoutManager as? SwipeLayoutManager)?.stackTopPos ?: START_POS
        outState.putInt(POS_KEY_BUNDLE, topPos)
        outState.putLong(ID_KEY_BUNDLE, dictionaryId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearViews()
    }

    private fun setupClickListeners() {
        backToDictionariesBtn?.setOnClickListener { requireActivity().onBackPressed() }
        continueTrainingBtn?.setOnClickListener { viewModel.loadDict(dictionaryId, onlyUnlearned = false) }
    }

    private fun initViews(view: View) {
        cardsStack = view.findViewById(R.id.cards_stack)
        trainingReportLayout = view.findViewById(R.id.layout_report_frame)
        backToDictionariesBtn = view.findViewById(R.id.back_button)
        continueTrainingBtn = view.findViewById(R.id.repeat_button)
        reportText = view.findViewById(R.id.text_after_complete)
    }

    private fun clearViews() {
        cardsStack = null
        trainingReportLayout = null
        backToDictionariesBtn = null
        continueTrainingBtn = null
        reportText = null
    }

    private fun handleScoreState(state: ScoreState) {
        when (state) {
            is ScoreState.Hide -> {
                trainingReportLayout?.isVisible = false
                cardsStack?.isVisible = true
            }

            is ScoreState.Show -> {
                cardsStack?.isVisible = false
                scoreAnimation.startAppearance(trainingReportLayout, APPEARANCE_DURATION)
                reportText?.text = state.text
            }
        }
    }

    companion object {
        private const val RELATIVE_HEIGHT = 0.5f
        private const val APPEARANCE_DURATION = 500L
        private const val START_POS = 0
        private const val POS_KEY_BUNDLE = "top_pos"
        private const val ID_KEY_BUNDLE = "id"
        private const val CARDS_ON_SCREEN = 5

        fun newInstance(dictionaryId: Long) = TrainFlashcardsFragment().apply {
            arguments = Bundle().apply {
                putLong(ID_KEY_BUNDLE, dictionaryId)
            }
        }
    }
}
