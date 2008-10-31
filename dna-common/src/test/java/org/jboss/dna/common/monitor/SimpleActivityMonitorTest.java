/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.common.monitor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.util.EmptyIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.slf4j.Marker;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class SimpleActivityMonitorTest {

    private static final String VALID_TASK_NAME = "Checking for file";

    private ActivityMonitor monitor;
    @Mock
    Marker marker;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        stub(marker.iterator()).toReturn(new EmptyIterator<Marker>());
        this.monitor = new SimpleActivityMonitor(MockI18n.passthrough, "Reading from file X");
    }

    @Test
    public void shouldNotBeCancelledUponCreation() {
        assertThat(monitor.isCancelled(), is(false));
    }

    @Test
    public void shouldReturnActivityStatusWithEmptyMessageBeforeTaskIsBegun() {
        ActivityStatus status = monitor.getStatus(Locale.FRANCE);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is(""));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));

        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is(""));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
    }

    @Test
    public void shouldReturnActivityStatusWithCorrectMessageAfterTaskIsBegun() {
        monitor.beginTask(100, I18nMessages.testTaskName);
        monitor.worked(10.0d);
        ActivityStatus status = monitor.getStatus(Locale.FRANCE);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("examinez le message"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));

        status = monitor.getStatus(Locale.ENGLISH);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("test task"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));

        status = monitor.getStatus(Locale.getDefault());
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("test task"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));

        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("test task"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));
    }

    @Test
    public void shouldReturnActivityStatusWithCorrectMessageAndSubstitutedParametersAfterTaskIsBegun() {
        monitor.beginTask(100, I18nMessages.testTaskName2, 2);
        monitor.worked(10.0d);
        ActivityStatus status = monitor.getStatus(Locale.FRANCE);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("examinez le message 2"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));

        status = monitor.getStatus(Locale.ENGLISH);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("second test task 2"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));

        status = monitor.getStatus(Locale.getDefault());
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("second test task 2"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));

        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getTaskName(), is("second test task 2"));
        assertThat(status.getPercentWorked(), is(closeTo(10.0d, 0.001d)));
    }

    @Test
    public void shouldHaveProgressOfZeroPercentUponCreation() {
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(""));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
    }

    @Test
    public void shouldHaveProgressOfZeroPercentUponBeginningTask() {
        this.monitor.beginTask(100, MockI18n.passthrough, VALID_TASK_NAME);
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
    }

    @Test
    public void shouldShowProperStatus() {
        this.monitor.beginTask(1000, MockI18n.passthrough, VALID_TASK_NAME);
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
        for (int i = 1; i <= 9; ++i) {
            this.monitor.worked(100);
            // Check the monitor's status ...
            status = monitor.getStatus(null);
            assertThat(status, is(notNullValue()));
            assertThat(status.getActivityName(), is(monitor.getActivityName()));
            assertThat(status.getTaskName(), is(VALID_TASK_NAME));
            assertThat(status.getPercentWorked(), is(closeTo(10 * i, 0.001d)));
            assertThat(status.isDone(), is(false));
        }
        monitor.done();
        // Check the monitor's status shows 100%
        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(100, 0.001d)));
        assertThat(status.isDone(), is(true));
    }

    @Test
    public void shouldShowProperStatusUsingSubtasks() {
        monitor.beginTask(1000, MockI18n.passthrough, VALID_TASK_NAME);
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));

        // Create subtasks ...
        for (int i = 1; i <= 9; ++i) {
            ActivityMonitor subtask = monitor.createSubtask(100);
            assertThat(subtask, is(notNullValue()));
            assertThat(subtask, is(instanceOf(SubActivityMonitor.class)));
            assertThat(((SubActivityMonitor)subtask).getParent(), is(sameInstance(monitor)));

            String subtaskName = "Subtask " + i;
            subtask.beginTask(10, MockI18n.passthrough, subtaskName); // note the different total work for the subtask
            for (int j = 1; j <= 10; ++j) {
                // Work the subtask
                subtask.worked(1);

                // Check the submonitor's status
                status = subtask.getStatus(null);
                assertThat(status, is(notNullValue()));
                assertThat(status.getActivityName(), is(monitor.getActivityName()));
                assertThat(status.getTaskName(), is(subtaskName));
                assertThat(status.getPercentWorked(), is(closeTo(10 * j, 0.001d)));
                assertThat(status.isDone(), is(j == 10));

                // System.out.println(status);
            }
            subtask.done();

            // Check the main monitor's status
            status = monitor.getStatus(null);
            assertThat(status, is(notNullValue()));
            assertThat(status.getActivityName(), is(monitor.getActivityName()));
            assertThat(status.getTaskName(), is(VALID_TASK_NAME));
            assertThat(status.getPercentWorked(), is(closeTo(10 * i, 0.001d)));
            assertThat(status.isDone(), is(false));
        }
        monitor.done();

        // Check the monitor's status shows 100%
        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(100, 0.001d)));
        assertThat(status.isDone(), is(true));
    }

    @Test
    public void shouldAllowDoneToBeCalledEvenAfterFinished() {
        monitor.beginTask(1000, MockI18n.passthrough, VALID_TASK_NAME);
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
        assertThat(status.isDone(), is(false));

        for (int i = 0; i != 3; ++i) {
            // Just mark it as done ...
            monitor.done();

            // Check the status ...
            status = monitor.getStatus(null);
            assertThat(status, is(notNullValue()));
            assertThat(status.getActivityName(), is(monitor.getActivityName()));
            assertThat(status.getTaskName(), is(VALID_TASK_NAME));
            assertThat(status.getPercentWorked(), is(closeTo(100, 0.001d)));
            assertThat(status.isDone(), is(true));
        }
    }

    @Test
    public void shouldNotBeMarkedAsDoneAfterCancel() {
        monitor.beginTask(100, MockI18n.passthrough, VALID_TASK_NAME);
        monitor.setCancelled(true);
        assertThat(monitor.isCancelled(), is(true));
        assertThat(monitor.isDone(), is(false));
    }

    @Test
    public void shouldAllowCancelToBeRejected() {
        monitor.beginTask(1000, MockI18n.passthrough, VALID_TASK_NAME);
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
        for (int i = 1; i <= 9; ++i) {
            monitor.worked(100);

            // Check the monitor's status ...
            status = monitor.getStatus(null);
            assertThat(status, is(notNullValue()));
            assertThat(status.getActivityName(), is(monitor.getActivityName()));
            assertThat(status.getTaskName(), is(VALID_TASK_NAME));
            assertThat(status.getPercentWorked(), is(closeTo(10 * i, 0.001d)));
            assertThat(status.isDone(), is(false));

            // Cancel the activity ...
            monitor.setCancelled(true);
            assertThat(monitor.isCancelled(), is(true));
        }
        monitor.done();
        // Check the monitor's status shows 100%
        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(100, 0.001d)));
        assertThat(status.isDone(), is(true));

    }

    @Test
    public void shouldContinueToRecordWorkEvenWhenCancelled() {
        monitor.beginTask(1000, MockI18n.passthrough, VALID_TASK_NAME);
        ActivityStatus status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(0.0d, 0.001d)));
        for (int i = 1; i <= 9; ++i) {
            monitor.worked(100);

            // Check the monitor's status ...
            status = monitor.getStatus(null);
            assertThat(status, is(notNullValue()));
            assertThat(status.getActivityName(), is(monitor.getActivityName()));
            assertThat(status.getTaskName(), is(VALID_TASK_NAME));
            assertThat(status.getPercentWorked(), is(closeTo(10 * i, 0.001d)));
            assertThat(status.isDone(), is(false));

            // Cancel the activity ...
            monitor.setCancelled(true);
            assertThat(monitor.isCancelled(), is(monitor.isCancelled()));
        }
        monitor.done();
        // Check the monitor's status shows 100%
        status = monitor.getStatus(null);
        assertThat(status, is(notNullValue()));
        assertThat(status.getActivityName(), is(monitor.getActivityName()));
        assertThat(status.getTaskName(), is(VALID_TASK_NAME));
        assertThat(status.getPercentWorked(), is(closeTo(100, 0.001d)));
        assertThat(status.isDone(), is(true));
        assertThat(monitor.isCancelled(), is(true));
    }

    @Test
    public void shouldAllowCaptureInformation() {
        monitor.capture(MockI18n.passthrough, "Message");
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureInformationWithNoMessage() {
        monitor.capture(null);
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCaptureInformationWithNoMessageAndMessageParameters() {
        monitor.capture(null, "message");
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureInformationWithMarker() {
        monitor.capture(marker, null);
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureError() {
        monitor.captureError(MockI18n.passthrough, "Error");
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureErrorWithNoMessage() {
        monitor.captureError(null);
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCaptureErrorWithNoMessageAndMessageParameters() {
        monitor.captureError(null, "Error");
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureErrorWithMarker() {
        monitor.captureError(marker, null);
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureErrorWithThrowable() {
        monitor.captureError(new Throwable());
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureWarning() {
        monitor.captureWarning(MockI18n.passthrough, "Warning");
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureWarningWithNoMessage() {
        monitor.captureWarning(null);
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCaptureWarningWithNoMessageAndMessageParameters() {
        monitor.captureWarning(null, "Warning");
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureWarningWithMarker() {
        monitor.captureWarning(marker, null);
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    @Test
    public void shouldAllowCaptureWarningWithThrowable() {
        monitor.captureWarning(new Throwable());
        assertThat(monitor.getStatus(Locale.getDefault()).getCapturedInformation().length > 0, is(true));
    }

    public static class I18nMessages {

        public static I18n testTaskName;
        public static I18n testTaskName2;

        static {
            try {
                I18n.initialize(SimpleActivityMonitorTest.I18nMessages.class);
            } catch (final Exception err) {
                System.err.println(err);
            }
        }

        public static Set<Locale> getLocalizationProblemLocales() {
            return I18n.getLocalizationProblemLocales(CommonI18n.class);
        }

        public static Set<String> getLocalizationProblems() {
            return I18n.getLocalizationProblems(CommonI18n.class);
        }

        public static Set<String> getLocalizationProblems( Locale locale ) {
            return I18n.getLocalizationProblems(CommonI18n.class, locale);
        }

    }
}
