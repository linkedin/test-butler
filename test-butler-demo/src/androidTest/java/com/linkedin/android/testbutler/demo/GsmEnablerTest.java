/**
 * Copyright (C) 2016 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.testbutler.demo;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.test.InstrumentationRegistry;

import com.linkedin.android.testbutler.TestButler;
import com.linkedin.android.testbutler.demo.utils.Waiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

public class GsmEnablerTest {
    public static final int RETRY_COUNT = 10;
    public static final int PAUSE = 100;
    private ConnectivityManager connectivityManager;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @After
    public void teardown() {
        //Restore
        enableDataTransfer();
    }

    @Test
    public void disableGsmDataTransmission() {
        //Precondition
        boolean precondition;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        precondition = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        assertTrue("Data transfer was not enabled at the beginning of the test", precondition);

        //Action
        disableDataTransferAndCheck();
    }


    @Test
    public void disableAndEnableGsmDataTransmission() {
        //Precondition
        boolean precondition;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        precondition = activeNetworkInfo != null && activeNetworkInfo.isConnected();
        assertTrue("Data transfer was not enabled at the beginning of the test", precondition);

        //Action 1
        disableDataTransferAndCheck();

        //Action 2
        enableDataTransferAndCheck();
    }

    private void disableDataTransferAndCheck() {
        boolean result = disableDataTransfer();

        assertTrue("Data transfer was not disabled", result);
    }

    private boolean disableDataTransfer() {
        //Action
        TestButler.setGsmState(false);

        //Check
        return Waiter.wait(RETRY_COUNT, new Waiter.DelayDependOnCount.SimpleLinearDelay(PAUSE), new Waiter.Predicate() {
            @Override
            public boolean compute(int tryCount) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
            }
        });
    }

    private void enableDataTransferAndCheck() {
        boolean result = enableDataTransfer();

        assertTrue("Data transfer was not enabled", result);
    }

    private boolean enableDataTransfer() {
        //Action
        TestButler.setGsmState(true);

        //Check
        return Waiter.wait(RETRY_COUNT, new Waiter.DelayDependOnCount.SimpleLinearDelay(PAUSE), new Waiter.Predicate() {
            @Override
            public boolean compute(int tryCount) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        });
    }
}
