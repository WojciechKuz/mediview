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
 *
 * Correct usage:
 * ```
 * allQueue.startJob(condition) {
 *     someJob.invokeOnCompletion { allQueue.finishJob() }
 * }
 * ```
 */
class LaunchQueue {
    var waitingJob: MyJob? = null
    var working = false

    /** @param queueWhen if true add to queue, else execute immediately. */
    fun startJob(queueWhen: Boolean, task: MyJob) {
        if(!queueWhen) {
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