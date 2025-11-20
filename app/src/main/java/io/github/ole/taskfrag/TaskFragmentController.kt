package io.github.ole.taskfrag

import android.app.Activity
import android.app.WindowConfiguration
import android.content.Intent
import android.graphics.Rect
import android.os.Binder
import android.window.TaskFragmentCreationParams
import android.window.TaskFragmentInfo
import android.window.TaskFragmentOperation
import android.window.TaskFragmentOrganizer
import android.window.TaskFragmentTransaction
import android.window.WindowContainerTransaction
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import kotlin.apply
import kotlin.let

typealias FragmentInfoCallback = (TaskFragmentInfo) -> Unit
typealias FragmentGoneCallback = () -> Unit

class TaskFragmentController(
    private val activity: Activity,
    private val onCreateCallback: FragmentInfoCallback,
    private val onInfoChangedCallback: FragmentInfoCallback,
    private val goneCallback: FragmentGoneCallback,
    executor: Executor
) {
    private val fragmentToken = Binder()
    private var fragmentInfo: TaskFragmentInfo? = null

    class Organizer(val component: WeakReference<TaskFragmentController>, executor: Executor) :
        TaskFragmentOrganizer(executor) {

        override fun onTransactionReady(transaction: TaskFragmentTransaction) {
            component.get()?.handleTransactionReady(transaction)
        }
    }

    private val organizer: TaskFragmentOrganizer =
        Organizer(WeakReference(this), executor).apply {
            registerOrganizer(true /* isSystemOrganizer */)
        }

    private fun handleTransactionReady(transaction: TaskFragmentTransaction) {
        val resultT = WindowContainerTransaction()

        for (change in transaction.changes) {
            change.taskFragmentInfo?.let { taskFragmentInfo ->
                if (taskFragmentInfo.fragmentToken == fragmentToken) {
                    fragmentInfo = taskFragmentInfo
                    when (change.type) {
                        TaskFragmentTransaction.TYPE_TASK_FRAGMENT_APPEARED -> {
                            resultT.addTaskFragmentOperation(
                                fragmentToken,
                                TaskFragmentOperation.Builder(
                                    TaskFragmentOperation.OP_TYPE_REORDER_TO_TOP_OF_TASK
                                ).build(),
                            )
                            onCreateCallback(taskFragmentInfo)
                        }
                        TaskFragmentTransaction.TYPE_TASK_FRAGMENT_INFO_CHANGED -> {
                            onInfoChangedCallback(taskFragmentInfo)
                        }
                        TaskFragmentTransaction.TYPE_TASK_FRAGMENT_VANISHED -> {
                            goneCallback()
                        }
                        TaskFragmentTransaction.TYPE_TASK_FRAGMENT_PARENT_INFO_CHANGED -> {}
                        TaskFragmentTransaction.TYPE_TASK_FRAGMENT_ERROR -> {
                            goneCallback()
                        }
                        TaskFragmentTransaction.TYPE_ACTIVITY_REPARENTED_TO_TASK -> {}
                        else ->
                            throw kotlin.IllegalArgumentException(
                                "Unknown TaskFragmentEvent=" + change.type
                            )
                    }
                }
            }
        }
        organizer.onTransactionHandled(
            transaction.transactionToken,
            resultT,
            TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_CHANGE,
            false,
        )
    }

    /** Creates the task fragment */
    fun createTaskFragment() {
        val fragmentOptions =
            TaskFragmentCreationParams.Builder(
                organizer.organizerToken,
                fragmentToken,
                activity.activityToken!!,
            )
                .setInitialRelativeBounds(Rect())
                .setWindowingMode(WindowConfiguration.WINDOWING_MODE_FULLSCREEN)
                .build()
        organizer.applyTransaction(
            WindowContainerTransaction().createTaskFragment(fragmentOptions),
            TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_CHANGE,
            false,
        )
    }

    private fun WindowContainerTransaction.startActivity(intent: Intent) =
        this.startActivityInTaskFragment(fragmentToken, activity.activityToken!!,
            intent, null)

    /** Starts the provided activity in the fragment and move it to the background */
    fun startActivityInTaskFragment(intent: Intent) {
        organizer.applyTransaction(
            WindowContainerTransaction().startActivity(intent),
            TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_OPEN,
            false,
        )
    }

    fun isFragmentEmpty(): Boolean {
        return fragmentInfo?.isEmpty ?: true
    }

    fun isFragmentOnTop(): Boolean {
        return fragmentInfo?.isTopNonFinishingChild ?: false
    }

    fun lastActivityRemaining(): Boolean {
        return (fragmentInfo?.runningActivityCount ?: 0) == 1
    }

    fun reorderToTop() {
        organizer.applyTransaction(
            WindowContainerTransaction().addTaskFragmentOperation(
                fragmentToken,
                TaskFragmentOperation.Builder(
                    TaskFragmentOperation.OP_TYPE_REORDER_TO_TOP_OF_TASK
                ).build(),
            ),
            TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_CHANGE,
            false,
        )
    }

    fun reorderToBottom() {
        organizer.applyTransaction(
            WindowContainerTransaction().addTaskFragmentOperation(
                fragmentToken,
                TaskFragmentOperation.Builder(
                    TaskFragmentOperation.OP_TYPE_REORDER_TO_BOTTOM_OF_TASK
                ).build(),
            ),
            TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_CHANGE,
            false,
        )
    }

    fun moveTaskFragment(rect: Rect) {
        fragmentInfo?.token?.let { token ->
            organizer.applyTransaction(
                WindowContainerTransaction().setRelativeBounds(
                    token, rect
                ),
                TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_CHANGE,
                false,
            )
        }
    }

    fun destroyTaskFragment() {
        organizer.applyTransaction(
            WindowContainerTransaction().addTaskFragmentOperation(
                fragmentToken,
                TaskFragmentOperation.Builder(
                    TaskFragmentOperation.OP_TYPE_DELETE_TASK_FRAGMENT
                ).build(),
            ),
            TaskFragmentOrganizer.TASK_FRAGMENT_TRANSIT_CLOSE,
            false,
        )
    }

    /** Destroys the task fragment */
    fun destroy() {
        destroyTaskFragment()
        organizer.unregisterOrganizer()
    }
}
