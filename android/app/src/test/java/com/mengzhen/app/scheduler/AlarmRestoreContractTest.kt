package com.mengzhen.app.scheduler

import com.mengzhen.app.data.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRestoreContractTest {

    @Test
    fun `pending unarmed player session is not restored`() {
        assertFalse(
            AlarmRestoreContract.shouldRestore(
                status = TaskStatus.PENDING,
                scheduleArmed = false,
            )
        )
    }

    @Test
    fun `executing session is restored even when legacy state is unarmed`() {
        assertTrue(
            AlarmRestoreContract.shouldRestore(
                status = TaskStatus.EXECUTING,
                scheduleArmed = false,
            )
        )
    }

    @Test
    fun `armed pending task is restored`() {
        assertTrue(
            AlarmRestoreContract.shouldRestore(
                status = TaskStatus.PENDING,
                scheduleArmed = true,
            )
        )
    }
}
