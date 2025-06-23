import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import transform3d.ExtView

fun interface MyJob {
    fun job(): Unit
}

/**
 * If job's View is ExtView.FREE, queue 1 job. Else execute immediately.
 * For ExtView.FREE:
 *
 * Does nothing? Start new job.
 * Already does something? Place as next to do.
 *
 * Finished, and there's next job? Start next job.
 * Finished, and no new job? Set does nothing.
 */
object LaunchQueue {
    var waitingJob: MyJob? = null
    var working = false

    fun startJob(view: ExtView, task: MyJob) {
        if(view != ExtView.FREE) {
            task.job()
            return
        }
        if(!working) {
            working = true
            task.job()
        } else {
            waitingJob = task
        }
    }
    fun finishJob() {
        if(waitingJob != null) {
            waitingJob!!.job()
            waitingJob = null
        } else {
            working = false
        }
    }
}