package com.example.cameraapp

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.beep.trade.view.fragments.BaseBottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract public class BaseFragment : Fragment(), View.OnTouchListener {

    companion object {
        private val TAG : String = BaseFragment::class.java.getSimpleName()
    }

    private var inputMethodManager : InputMethodManager? = null
    //region Log Methods
    protected fun logDebug(TAG : String, message : String) {
        Log.d(TAG, message)
    }

    protected fun logError(TAG : String, message : String?) {
        Log.e(TAG, message ?: Constants.NIL)
    }

    protected fun logError(TAG : String, message : String?, throwable : Throwable) {
        Log.e(TAG, message ?: Constants.NIL, throwable)
    }
    //endregion
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inputMethodManager = getInputMethodManager()
        Coroutines.main(this@BaseFragment, { onSetObservers(it) })
    }

    protected abstract suspend fun onSetObservers(scope : CoroutineScope)

    protected fun showToast(messageToast : String?, duration : Int = Toast.LENGTH_SHORT) { logDebug(TAG,"showToast($messageToast)")
        Coroutines.main(this@BaseFragment, { scope -> scope.launch( block = {
            if (messageToast?.isNotBlank() == true) Toast.makeText(requireContext(), messageToast, duration).show()
        }) })
    }

    protected fun showSnackBar(messageToast : String, duration : Int = Snackbar.LENGTH_SHORT) { logDebug(TAG, "showSnackBar($messageToast)")
        Coroutines.main(this@BaseFragment, { scope -> scope.launch( block = {
            Snackbar.make(requireView(), messageToast, duration).show()
        }) })
    }

    protected fun getColor(id : Int) : Int {
        return ContextCompat.getColor(requireContext(), id)
    }
    //region Get String Methods
    protected fun getString(id : Int, value : String?) : String {
        return String.format(requireContext().getResources().getString(id, value))
    }

    protected fun getString(id : Int, value : Int) : String {
        return String.format(requireContext().getResources().getString(id, getString(value)))
    }
    //endregion
    //region Soft Keyboard Methods
    protected fun getInputMethodManager() : InputMethodManager { logDebug(TAG,"getInputMethodManager()")
        return requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    protected fun showSoftKeyboard(view : View?) { logDebug(TAG,"showSoftKeyboard($view)")
        Coroutines.main(this@BaseFragment, work = {
            inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        })
    }

    protected fun hideSoftKeyboard() { logDebug(TAG,"hideSoftKeyboard()")
        inputMethodManager?.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    protected fun hideSoftKeyboard(view : View?) { logDebug(TAG,"hideSoftKeyboard($view)")
        inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
    //endregion
    //region On Touch Methods
    private lateinit var rect : Rect
    protected var isActionDown : Boolean = false
    protected var isActionMove : Boolean = false
    protected var isActionUp : Boolean = false
    private var dx : Int = 0
    private var dy : Int = 0

    override fun onTouch(view : View, event : MotionEvent) : Boolean {
        logDebug(TAG,"onTouch($view,$event)")
        isActionDown = (event.getAction() == MotionEvent.ACTION_DOWN)
        isActionMove = (event.getAction() == MotionEvent.ACTION_MOVE)
        isActionUp = (event.getAction() == MotionEvent.ACTION_UP)
        dx = event.getX().toInt()
        dy = event.getY().toInt()
        return if (isActionDown) setRect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom())
        else onTouchFragment(view, event)
    }

    open fun onTouchFragment(view : View, event : MotionEvent) : Boolean {
        logDebug(TAG,"onTouchFragment($view, $event)")
        return isActionUp.not()
    }

    protected fun setRect(left : Int, top : Int, right : Int, bottom : Int) : Boolean {
        rect = Rect(left, top, right, bottom)
        return true
    }

    protected fun isInsideBounds(view : View) : Boolean {
        return rect.contains(view.left + dx, view.top + dy)
    }
    //endregion
    //region Transition Manager  Methods
    protected fun delayTransition(constraintLayoutMain : ConstraintLayout?, duration : Long = 250) {
        val transition = AutoTransition()
        transition.setDuration(duration) //Delay to animation duration
        constraintLayoutMain?.let { //Delay to animate
            TransitionManager.beginDelayedTransition(it, transition)
        }
    }
    //endregion
    //region Child Fragment Manager Methods
    protected fun replaceFragment(containerViewId : Int, fragment : Fragment) {
        logDebug(TAG,"replaceFragment($containerViewId,$fragment)")
        getChildFragmentManager().beginTransaction()
            .replace(containerViewId, fragment, fragment.getTag())
            .commitNow()
    }

    protected fun addToBackStackFragment(containerViewId : Int, fragment : Fragment) {
        logDebug(TAG, "addToBackStackFragment($containerViewId,$fragment)")
        getChildFragmentManager().beginTransaction()
            .add(containerViewId, fragment)
            .addToBackStack(fragment.getTag())
            .commit()
    }

    protected fun showBottomSheetFragment(bottomSheetDialogFragment : BaseBottomSheetDialogFragment, tag : String? = null) {
        if (getActivity() != null && requireActivity().getSupportFragmentManager().isDestroyed() == false) {
            bottomSheetDialogFragment.show(
                requireActivity().getSupportFragmentManager(), tag?:bottomSheetDialogFragment.javaClass.name
            )
        }
    }

    protected fun getBackStack() : Int {
        return requireActivity().getSupportFragmentManager().getBackStackEntryCount()
    }

    protected fun popBackStack() { logDebug(TAG, "popBackStack()")
        when (getChildFragmentManager().getBackStackEntryCount()) {
            0 -> { logDebug(TAG,"super.onBackPressed()")
                logDebug(TAG, "onBackPressed.backStackEntryCount ${getChildFragmentManager().getBackStackEntryCount()}")
            }
            else -> { logDebug(TAG,"getChildFragmentManager().popBackStack()")
                logDebug(TAG, "backStackEntryCount ${getChildFragmentManager().getBackStackEntryCount()}")
                getChildFragmentManager().popBackStack()
            }
        }
    }

    protected fun popBackStack(fragment : Fragment) { logDebug(TAG, "popBackStack($fragment)")
        getChildFragmentManager().popBackStack(fragment.getTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    protected fun popAllBackStack() { logDebug(TAG, "popAllBackStack()")
        getChildFragmentManager().getFragments().map { fragment ->
            getChildFragmentManager().popBackStack(fragment.getTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    //endregion
    protected fun onBackPressed() {
        requireActivity().onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}